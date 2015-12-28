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
package us.jubat.yarn.applicationmaster

import java.io.File
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync
import org.apache.hadoop.yarn.api.ApplicationConstants
import us.jubat.yarn.common.HasLogger

import scala.collection.JavaConverters._
import scala.collection._
import org.apache.hadoop.yarn.api.records._
import org.apache.hadoop.yarn.util.{Apps, ConverterUtils, Records}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment
import org.apache.hadoop.yarn.client.api.NMClient
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest
import scala.io.Source
import java.util

class ApplicationMaster extends HasLogger {

  private def entryScriptPath(aBasePath: Path): Path = new Path(aBasePath, "container/entrypoint.sh")
  private val entryScriptName: String = "entrypoint.sh"
  private def containerJarPath(aBasePath: Path): Path = new Path(aBasePath, "container/jubatus-on-yarn-container.jar")
  private val containerJarName: String = "jubatus-on-yarn-container.jar"
  private val containerMainClass: String = "us.jubat.yarn.container.ContainerApp"

  private val mYarnConfig = new YarnConfiguration()

  // 終了までブロックする
  def run(aParams: ApplicationMasterParams, aApplicationMasterPort: Int): FinalApplicationStatus = {
    logger.debug(s"ApplicationMasterParams (${aParams.toString()})")

    val tHandler = new ApplicationMasterHandler(aParams, aApplicationMasterPort)

    val tResourceManager = AMRMClientAsync.createAMRMClientAsync[ContainerRequest](500, tHandler)
    tResourceManager.init(mYarnConfig)
    tResourceManager.start()

    tResourceManager.registerApplicationMaster("", 0, "") // XXX: registerApplicationMaster にわたす引数は使われていないらしい。

    val tPriority = Records.newRecord(classOf[Priority])
    tPriority.setPriority(aParams.priority)

    val tResource = Records.newRecord(classOf[Resource])
    tResource.setMemory(aParams.memory + aParams.containerMemory)
    tResource.setVirtualCores(aParams.virtualCores)

    var containerNodes: Array[String] = null
    var containerRacks: Array[String] = null
    if (!aParams.containerNodes.isEmpty()) {
      containerNodes = aParams.containerNodes.split(",").toArray[String]
    }
    if (!aParams.containerRacks.isEmpty()) {
      containerRacks = aParams.containerRacks.split(",").toArray[String]
    }

    // コンテナを起動
    (1 to aParams.nodes).foreach { _ =>
      logger.info(
        "add container\n"
        + s"\tpriority: $tPriority\n"
        + s"\tmemory: ${tResource.getMemory}\n"
        + s"\tvirtualCores: ${tResource.getVirtualCores}"
      )
      val containerReq = new ContainerRequest(tResource, containerNodes, containerRacks, tPriority)
      tResourceManager.addContainerRequest(containerReq)
      logger.debug(s"ContainerRequest( Nodes:${containerReq.getNodes}, Racks:${containerReq.getRacks}")
    }

    // コンテナの終了を待機
    while (!tHandler.isFinished) {
      Thread.sleep(500)
    }
    logger.info("all containers are finished")

    // ApplicationMaster を終了
    val tStatus = if (tHandler.isFailed) FinalApplicationStatus.FAILED else FinalApplicationStatus.SUCCEEDED
    logger.info(s"finish ApplicationMaster with $tStatus")
    tResourceManager.unregisterApplicationMaster(tStatus, "", "")
    tStatus
  }

  private class ApplicationMasterHandler(aParams: ApplicationMasterParams, aApplicationMasterPort: Int) extends AMRMClientAsync.CallbackHandler with HasLogger {
    private val mNodeManager = NMClient.createNMClient()
    mNodeManager.init(mYarnConfig)
    mNodeManager.start()

    private val mLock = new AnyRef
    private var mCompletedContainerCount = 0
    private var mStartContainerCount = 0
    private val mMaxContainerCount = aParams.nodes

    var mErrorContainerCount = 0

    def isFinished: Boolean = mCompletedContainerCount >= mMaxContainerCount

    def isFailed: Boolean = mErrorContainerCount > 0

    def finish(): Unit = mCompletedContainerCount = mMaxContainerCount

    private def toLocalResource(aPath: Path): LocalResource = {
      val tResource = Records.newRecord(classOf[LocalResource])
      val tFileStatus = FileSystem.get(mYarnConfig).getFileStatus(aPath)
      tResource.setResource(ConverterUtils.getYarnUrlFromPath(aPath))
      tResource.setSize(tFileStatus.getLen)
      tResource.setTimestamp(tFileStatus.getModificationTime)
      tResource.setType(LocalResourceType.FILE)
      tResource.setVisibility(LocalResourceVisibility.PUBLIC)
      tResource
    }

    override def onContainersAllocated(aContainers: java.util.List[Container]): Unit = {
      aContainers.asScala.foreach { tContainer =>
        val tLaunchContext = Records.newRecord(classOf[ContainerLaunchContext])

        // LocalResource
        val localResource = mutable.Map.empty[String, LocalResource]
        localResource += entryScriptName -> toLocalResource(entryScriptPath(new Path(aParams.basePath)))
        localResource += containerJarName -> toLocalResource(containerJarPath(new Path(aParams.basePath)))
        val serverLogConfFileName = if (aParams.jubatusServerLogConf nonEmpty) {
             val logConfFileName = aParams.jubatusServerLogConf.split('/').last
             localResource +=  logConfFileName -> toLocalResource(new Path(aParams.jubatusServerLogConf))
             logConfFileName
        } else {
          """\"\""""
        }
        tLaunchContext.setLocalResources(localResource.asJava)

        // ClassPath
        val tApplicationMasterEnv = new java.util.HashMap[String, String]()
        mYarnConfig.getStrings(
          YarnConfiguration.YARN_APPLICATION_CLASSPATH,
          YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH: _*
        ).foreach { c =>
          Apps.addToEnvironment(tApplicationMasterEnv, Environment.CLASSPATH.name(), c.trim(), ":")
        }
        Apps.addToEnvironment(tApplicationMasterEnv, Environment.CLASSPATH.name(), Environment.PWD.$() + File.separator + "*", ":")
        tLaunchContext.setEnvironment(tApplicationMasterEnv)

        // Startup Command
        val tCommand = (
          s"bash $entryScriptName"
            + s" $containerJarName"
            + s" $containerMainClass"
            + s" ${aParams.containerMemory}"

            // jar にわたす
            + s" ${aParams.applicationName}"  // --application-name
            + s" $mStartContainerCount" // --seq
            + s" ${aParams.applicationMasterNodeAddress}" // --application-master-address
            + s" $aApplicationMasterPort" // --application-master-port

            // jub* にわたす
            + s" ${aParams.learningMachineName}"  // --name
            + s" ${aParams.learningMachineType}"  // juba{*}
            + s" ${aParams.zooKeepers}" // --zookeeper
            + s" ${aParams.thread}"  // --thread
            + s" ${aParams.timeout}"  // --timeout
            + s" ${aParams.mixer}"  // --mixer
            + s" ${aParams.intervalSec}"  // --interval_sec
            + s" ${aParams.intervalCount}"  // --interval_count
            + s" ${aParams.zookeeperTimeout}"  // --zookeeper_timeout
            + s" ${aParams.interconnectTimeout}"  // --interconnect_timeout
            + s" ${serverLogConfFileName}"   // --log_config

            + s" 1>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stdout"
            + s" 2>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stderr"
        )
        tLaunchContext.setCommands(List(tCommand).asJava)

        // Container Start
        logger.info(
          s"start Container\n"
            + s"\tid: ${tContainer.getId}\n"
            + s"\tcommand: $tCommand\n"
            + s"\tmemory: ${tContainer.getResource.getMemory}\n"
            + s"\tvirtualCores: ${tContainer.getResource.getVirtualCores}\n"
            + s"\taddress: ${tContainer.getNodeHttpAddress}"
        )
        mNodeManager.startContainer(tContainer, tLaunchContext)

        mLock.synchronized {
          mStartContainerCount = mStartContainerCount + 1
        }
      }
    }

    override def onContainersCompleted(aStatuses: java.util.List[ContainerStatus]): Unit = {
      aStatuses.asScala.foreach { tStatus =>
        logger.info(
          s"completed Container\n"
            + s"\tid: ${tStatus.getContainerId}\n"
            + s"\texitStatus: ${tStatus.getExitStatus}"
        )

        mLock.synchronized {
          mCompletedContainerCount = mCompletedContainerCount + 1
          if (tStatus.getExitStatus != 0) {
            mErrorContainerCount = mErrorContainerCount + 1
          }
        }
      }
    }

    override def onError(e: Throwable): Unit = {
      logger.warn(s"error in container\n ${e.getMessage}")
      finish()
    }

    override def getProgress: Float = mCompletedContainerCount / mMaxContainerCount

    override def onShutdownRequest(): Unit = {
      logger.info("container receive ShutdownRequest")
      finish()
    }

    override def onNodesUpdated(updatedNodes: java.util.List[NodeReport]): Unit = {}
  }
}
