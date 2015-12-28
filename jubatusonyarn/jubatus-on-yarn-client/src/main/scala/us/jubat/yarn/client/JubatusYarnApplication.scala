// Jubatus: Online machine learning framework for distributed environment
// Copyright (C) 2014-2015 Preferred Networks and Nippon Telegraph and Telephone Corporation.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License version 2.1 as published by the Free Software Foundation.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
package us.jubat.yarn.client

import org.apache.hadoop.fs.{Path, FileSystem}
import scala.concurrent.Future
import scala.util.{Success, Failure, Try}
import java.net.InetAddress
import us.jubat.yarn.common._
import scala.Some
import us.jubat.yarn.client.JubatusYarnApplication.ApplicationContext
import org.apache.hadoop.yarn.api.records.{FinalApplicationStatus, ApplicationReport}
import org.apache.hadoop.yarn.conf.YarnConfiguration

// TODO ExecutionContextをとりあえず追加。これで問題ないかあとで確認。

import scala.concurrent.ExecutionContext.Implicits.global

object Resource {
  val defaultMasterMemory: Int = 128
  val defaultJubatusProxyMemory: Int = 32
  val defaultMasterCores: Int = 1
  val defaultPriority: Int = 0
  val defaultContainerMemory: Int = 128
  val defaultJubatusServerMemory: Int = 256
  val defaultContainerCores: Int = 1
}
case class Resource(priority: Int = Resource.defaultPriority, memory: Int = Resource.defaultJubatusServerMemory,
    virtualCores: Int = Resource.defaultContainerCores, masterMemory: Int = Resource.defaultMasterMemory,
    proxyMemory: Int = Resource.defaultJubatusProxyMemory, masterCores: Int = Resource.defaultMasterCores,
    containerMemory: Int = Resource.defaultContainerMemory, containerNodes: List[String] = null, containerRacks: List[String] = null)

case class JubatusYarnApplicationStatus(jubatusProxy: java.util.Map[String, java.util.Map[String, String]], jubatusServers: java.util.Map[String, java.util.Map[String, String]], yarnApplication: java.util.Map[String, Any])

case class JubatusClusterConfiguration(learningMachineName: String, learningMachineType: LearningMachineType, zookeepers: List[Location], configString: String,
    configFile: Path = null, resource: Resource, nodeCount: Int, applicationName: String, serverConfig: ServerConfig, proxyConfig: ProxyConfig, basePath: Path = new Path("hdfs:///jubatus-on-yarn"))

object JubatusYarnApplication extends HasLogger {

  private case class ApplicationContext(controller: YarnClientController, proxy: ApplicationMasterProxy, service: JubatusYarnService)

  private def waitForStarted(aContext: ApplicationContext): JubatusYarnApplication = {
    // ApplicationMaster 上で JubatusProxy が起動し、すべての Container 上で jubatusServer が起動するまでブロック
    logger.info("wait for ApplicationMaster status 'Wait'")
    while (aContext.proxy.status != ControllerStatus.Wait) {
      aContext.controller.getFinalStatus match {
        case FinalApplicationStatus.FAILED =>
          aContext.controller.kill()
          throw new IllegalStateException("Application is failed.")
        case FinalApplicationStatus.KILLED =>
          aContext.controller.kill()
          throw new IllegalStateException("Application is killed.")
        case FinalApplicationStatus.SUCCEEDED =>
          aContext.controller.kill()
          throw new IllegalStateException("Application is finished.")
        case FinalApplicationStatus.UNDEFINED =>
      }
      Thread.sleep(100)
    }

    val tJubatusProxy = aContext.proxy.jubatusLocation.get

    val tJubatusServers = aContext.proxy.containers.values.map { tLocation =>
      tLocation.jubatusLocation.get
    }.toList

    logger.info(
      "new JubatusYarnApplication\n"
        + s"\n jubatusProxy: $tJubatusProxy\n"
        + s"\n jubatusServers: $tJubatusServers"
    )
    new JubatusYarnApplication(tJubatusProxy, tJubatusServers, aContext)
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigString config json string
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodeCount: Int): Future[JubatusYarnApplication] = {
    start(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigString, aResource, aNodeCount, new Path("hdfs:///jubatus-on-yarn"), null)
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigString config json string
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @param aApplicationName yarn-application name
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodeCount: Int, aApplicationName: String): Future[JubatusYarnApplication] = {
    start(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigString, aResource, aNodeCount, new Path("hdfs:///jubatus-on-yarn"), aApplicationName)
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigString config json string
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @param aBasePath base path of jar and sh files
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodeCount: Int, aBasePath: Path): Future[JubatusYarnApplication] = {
    start(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigString, aResource, aNodeCount, aBasePath, null)
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigString config json string
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @param aBasePath base path of jar and sh files
   * @param aApplicationName yarn-application name
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodeCount: Int, aBasePath: Path, aApplicationName: String): Future[JubatusYarnApplication] = Future {
    require(aResource.memory > 0, "specify memory than 1MB.")
    require(aNodeCount > 0, "specify node count than 1")

    val tService = new JubatusYarnService()
    tService.start()

    tService.yarnClientController match {
      case None => throw new IllegalStateException("Service not running.")
      case Some(tYarnClientController) =>
        // ApplicationMaster 起動
        logger.info(s"startJubatusApplication $aLearningMachineName, $aLearningMachineType, $aZookeepers, $aConfigString, $aResource, $aNodeCount, $aApplicationName")
        val tApplicationMasterProxy = tYarnClientController.startJubatusApplication(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigString, aResource, aNodeCount, aBasePath, aApplicationName)
        waitForStarted(ApplicationContext(tYarnClientController, tApplicationMasterProxy, tService))
    }
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aJubatusClusterConfiguration  argument of start method
   * @return  [[JubatusYarnApplication]]
   */
  def start(aJubatusClusterConfiguration: JubatusClusterConfiguration): Future[JubatusYarnApplication] = Future {

    val tService = new JubatusYarnService()
    tService.start()

    tService.yarnClientController match {
      case None => throw new IllegalStateException("Service not running.")
      case Some(tYarnClientController) =>
        // ApplicationMaster 起動
        logger.info(s"startJubatusApplication $aJubatusClusterConfiguration")
        val tApplicationMasterProxy = tYarnClientController.startJubatusApplication(aJubatusClusterConfiguration)
        waitForStarted(ApplicationContext(tYarnClientController, tApplicationMasterProxy, tService))
    }
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigFile config file
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodeCount: Int): Future[JubatusYarnApplication] = {
    start(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigFile, aResource, aNodeCount, new Path("hdfs:///jubatus-on-yarn"), null)
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigFile config file
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @param aApplicationName yarn-application name
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodeCount: Int, aApplicationName: String): Future[JubatusYarnApplication] = {
    start(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigFile, aResource, aNodeCount, new Path("hdfs:///jubatus-on-yarn"), aApplicationName)
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigFile config file
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodeCount: Int, aBasePath: Path): Future[JubatusYarnApplication] = {
    start(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigFile, aResource, aNodeCount, aBasePath,  null)
  }

  /**
   * JubatusYarnApplication を起動します。
   *
   * juba${aLearningMachineType_proxy} がひとつ, juba${aLearningMachineType} が ${aNodeCount} だけ起動します。
   * 各 juba${aLearningMachineType} の使用するリソースを ${aResource} に指定してください。
   *
   * @param aLearningMachineName  learning machine name
   * @param aLearningMachineType  learning machine type
   * @param aZookeepers ZooKeeper locations
   * @param aConfigFile config file
   * @param aResource  computer resources in the cluster
   * @param aNodeCount  number of cluster
   * @param aApplicationName yarn-application name
   * @return  [[JubatusYarnApplication]]
   */
  @deprecated("not recommended use the start(JubatusClusterConfiguration)")
  def start(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodeCount: Int, aBasePath: Path, aApplicationName: String): Future[JubatusYarnApplication] = Future {
    require(aResource.memory > 0, "specify memory than 1MB.")
    require(aNodeCount > 0, "specify node count than 1")

    val tService = new JubatusYarnService()
    tService.start()

    tService.yarnClientController match {
      case None => throw new IllegalStateException("Service not running.")
      case Some(tYarnClientController) =>
        // ApplicationMaster 起動
        logger.info(s"startJubatusApplication $aLearningMachineName, $aLearningMachineType, $aZookeepers, $aConfigFile, $aResource, $aNodeCount, $aApplicationName")
        val tApplicationMasterProxy = tYarnClientController.startJubatusApplication(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigFile, aResource, aNodeCount, aBasePath, aApplicationName)
        waitForStarted(ApplicationContext(tYarnClientController, tApplicationMasterProxy, tService))
    }
  }
}

class JubatusYarnApplication(val jubatusProxy: Location, val jubatusServers: List[Location], aContext: ApplicationContext) extends HasLogger {
  /**
   * JubatusYarnApplication のステータスを取得します。
   *
   * @return  [[JubatusYarnApplicationStatus]]
   */
  def status: JubatusYarnApplicationStatus = {
    logger.info("status")
    aContext.controller.status
  }

  /**
   * JubatusYarnApplication を停止します。
   *
   * ノード内で実行されている juba*, juba*_proxy のプロセスも停止します。
   */
  def stop(): Future[Unit] = Future {
    logger.info(s"stop ${aContext.proxy.name}")
    aContext.controller.stop() match {
      case Success(_) =>
      case Failure(e) =>
        throw e
    }

    while (!aContext.controller.isFinished) {
      Thread.sleep(100)
    }

    aContext.service.stop()
  }

  /**
   * JubatusYarnApplication を強制的に終了します。
   *
   * ノード内で実行されている juba*, juba*_proxy のプロセスも停止します。
   */
  def kill(): Unit = {
    logger.info("kill")
    aContext.controller.kill()
    aContext.service.stop()
  }

  /**
   * モデルデータを読み込みます。
   *
   * $aModelPathPrefix/$aModelId/$seq.jubatus からモデルデータを読み込みます。
   * $seq にはノード番号が入ります。
   *
   * @param aModelPathPrefix  HDFS path prefix
   */
  def loadModel(aModelPathPrefix: Path, aModelId: String): Try[JubatusYarnApplication] = Try {
    logger.info(s"loadModel $aModelPathPrefix, $aModelId")

    val tHdfs = FileSystem.get(new YarnConfiguration())
    val srcPath = new Path(aModelPathPrefix, aModelId)
    if (!tHdfs.exists(srcPath)) {
      val msg = s"model path does not exist ($srcPath)"
      logger.error(msg)
      throw new RuntimeException(msg)
    }

    for (i <- 0 to jubatusServers.size - 1) {
      val srcFile = new Path(srcPath, s"$i.jubatus")
      if (!tHdfs.exists(srcFile)) {
        val msg = s"model file does not exist ($srcFile)"
        logger.error(msg)
        throw new RuntimeException(msg)
      }
    }

    aContext.controller.loadModel(aModelPathPrefix, aModelId)
    this
  }

  /**
   * モデルデータを保存します。
   *
   * モデルデータは $aModelPathPrefix/$aModelId/$seq.jubatus に格納されます。
   * $seq にはノード番号が入ります。
   *
   * @param aModelPathPrefix  HDFS path prefix
   */
  def saveModel(aModelPathPrefix: Path, aModelId: String): Try[JubatusYarnApplication] = Try {
    logger.info(s"saveModel $aModelPathPrefix, $aModelId")
    aContext.controller.saveModel(aModelPathPrefix, aModelId)
    this
  }
}
