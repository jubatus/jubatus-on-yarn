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

import java.io.{FileInputStream, File, PrintWriter}
import java.util

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.yarn.api.ApplicationConstants
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment
import org.apache.hadoop.yarn.api.records.{Resource => YarnResource, _}
import org.apache.hadoop.yarn.client.api.{YarnClient => HadoopYarnClient}
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.util.{Apps, ConverterUtils, Records}
import us.jubat.yarn.common.{HasLogger, LearningMachineType, Location, ServerConfig, ProxyConfig}

import scala.collection.JavaConverters._
import scala.io.Source
import scala.collection._

trait YarnClient {
  def submitApplicationMaster(aJubatusClusterConfiguration: JubatusClusterConfiguration, aManagementLocation: Location): ApplicationId

  @deprecated("not recommended use the submitApplicationMaster(JubatusClusterConfiguration, Location)")
  def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId

  @deprecated("not recommended use the submitApplicationMaster(JubatusClusterConfiguration, Location)")
  def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId

  def getStatus(aApplicationId: ApplicationId): ApplicationReport

  def getFinalStatus(aApplicationId: ApplicationId): FinalApplicationStatus

  def kill(aApplicationId: ApplicationId): Unit
}

class DefaultYarnClient extends YarnClient with HasLogger {

  private def entryScriptPath(aBasePath: Path): Path = new Path(aBasePath, "application-master/entrypoint.sh")

  private val entryScriptName: String = "entrypoint.sh"

  private def jubaConfigBasePath(aBasePath: Path): Path = new Path(aBasePath, "application-master/jubaconfig")

  private val jubaConfigName: String = "jubaconfig.json"

  private def applicationMasterJarPath(aBasePath: Path): Path = new Path(aBasePath, "application-master/jubatus-on-yarn-application-master.jar")

  private val applicationMasterJarName: String = "jubatus-on-yarn-application-master.jar"
  private val applicationMasterMainClass: String = "us.jubat.yarn.applicationmaster.ApplicationMasterApp"

  private val jubatusProxyLogConfigFileName: String = "jubatusProxyLog4j.xml"
  private val applicationMasterLogConfigFileName: String = "applicationMasterLog4j.xml"

  private val mYarnConfig = new YarnConfiguration()
  private val mYarnClient = {
    val tYarnClient = HadoopYarnClient.createYarnClient()
    tYarnClient.init(mYarnConfig)
    tYarnClient.start()
    tYarnClient
  }

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

  @deprecated("not recommended use the submitApplicationMaster(JubatusClusterConfiguration, Location)")
  override def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId = {
    val tHdfsPath = new Path(jubaConfigBasePath(aBasePath), s"${aApplicationName.replaceAll(":", "_")}.json")

    val tTempFile = File.createTempFile(s"jubatus-on-yarn-server", ".json")
    val tWriter = new PrintWriter(tTempFile)
    tWriter.println(aConfigString)
    tWriter.close()

    FileSystem.get(mYarnConfig).copyFromLocalFile(true, true, new Path(tTempFile.getPath), tHdfsPath)

    submitApplicationMaster(aApplicationName, aLearningMachineInstanceName, aLearningMachineType, aZookeepers, tHdfsPath, aResource, aNodes, aManagementLocation, aBasePath)
  }

  @deprecated("not recommended use the submitApplicationMaster(JubatusClusterConfiguration, Location)")
  override def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId = {
    val resConfig = Resource(aResource.priority, aResource.memory, aResource.virtualCores, aResource.masterMemory, aResource.proxyMemory,
        aResource.masterCores, aResource.containerMemory, aResource.containerNodes, aResource.containerRacks, aNodes)
    val jubaClusterConfig = JubatusClusterConfiguration(aLearningMachineInstanceName, aLearningMachineType, aZookeepers, "",
      Some(aConfigFile), resConfig, aApplicationName, ServerConfig(), ProxyConfig(), None, aBasePath)
    submitApplicationMaster(jubaClusterConfig, aManagementLocation)
  }

  override def submitApplicationMaster(aJubatusClusterConfiguration: JubatusClusterConfiguration, aManagementLocation: Location): ApplicationId = {
    logger.info(s"call submitApplicationMaster($aJubatusClusterConfiguration, $aManagementLocation)")

    val configFile = aJubatusClusterConfiguration.configFile.getOrElse(createConfigFile(aJubatusClusterConfiguration.applicationName, aJubatusClusterConfiguration.configString, aJubatusClusterConfiguration.basePath))

    val localResource = mutable.Map.empty[String, LocalResource]
    localResource += entryScriptName -> toLocalResource(entryScriptPath(aJubatusClusterConfiguration.basePath))
    localResource += jubaConfigName -> toLocalResource(configFile)
    localResource += applicationMasterJarName -> toLocalResource(applicationMasterJarPath(aJubatusClusterConfiguration.basePath))
    val (applicationMasterLogConfigName, jubatusProxyLogConfigName, jubatusServerLogConfigPathString) = aJubatusClusterConfiguration.logConfig match {
      case Some(logConfig) =>
        val applicationMasterLogConfigNameString = logConfig.applicationMasterLogConfigPath match {
          case Some(applicationMasterLogConfigPath) =>
            localResource += applicationMasterLogConfigFileName -> toLocalResource(new Path(applicationMasterLogConfigPath))
            applicationMasterLogConfigFileName
          case None => """\"\""""
        }
        val jubatusProxyLogConfigNameString = logConfig.jubatusProxyLogConfigPath match {
          case Some(proxyLogConfigPath) =>
            localResource += jubatusProxyLogConfigFileName -> toLocalResource(new Path(proxyLogConfigPath))
            jubatusProxyLogConfigFileName
          case None => """\"\""""
        }
        val jubatusServerLogConfigPathString = logConfig.jubatusServerLogConfigPath match {
          case Some(serverLogConfigPath) =>
            serverLogConfigPath
          case None => """\"\""""
        }
        (applicationMasterLogConfigNameString, jubatusProxyLogConfigNameString, jubatusServerLogConfigPathString)
      case None => ("""\"\"""", """\"\"""", """\"\"""")
    }

    val tApplicationMasterContext = Records.newRecord(classOf[ContainerLaunchContext])
    tApplicationMasterContext.setLocalResources(localResource.asJava)

    // クラスパス
    val tApplicationMasterEnv = new util.HashMap[String, String]()
    mYarnConfig.getStrings(
      YarnConfiguration.YARN_APPLICATION_CLASSPATH,
      YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH: _*
    ).foreach { c =>
      Apps.addToEnvironment(tApplicationMasterEnv, Environment.CLASSPATH.name(), c.trim(), ":")
    }
    Apps.addToEnvironment(tApplicationMasterEnv, Environment.CLASSPATH.name(), Environment.PWD.$() + File.separator + "*", ":")
    tApplicationMasterContext.setEnvironment(tApplicationMasterEnv)

    // 起動コマンド
    def typeToString(aLearningMachineType: LearningMachineType): String = aLearningMachineType match {
      case LearningMachineType.Classifier => "classifier"
      case LearningMachineType.Anomaly => "anomaly"
      case LearningMachineType.Recommender => "recommender"
    }

    val containerNodes: String =
      if (aJubatusClusterConfiguration.resource.containerNodes.nonEmpty) s"""\"${aJubatusClusterConfiguration.resource.containerNodes.mkString(",")}\"""" else """\"\""""
    val containerRacks: String =
      if (aJubatusClusterConfiguration.resource.containerRacks.nonEmpty) s"""\"${aJubatusClusterConfiguration.resource.containerRacks.mkString(",")}\"""" else """\"\""""

    val tCommand = (
      s"bash $entryScriptName"
        // ApplicationMaster の jar 起動用 java コマンド
        + s" $applicationMasterJarName"
        + s" $applicationMasterMainClass"
        + s" ${aJubatusClusterConfiguration.resource.masterMemory}"

        // ApplicationMaster
        + s" ${aJubatusClusterConfiguration.applicationName}" // --application-name
        + s" ${aManagementLocation.hostAddress}" // --management-address
        + s" ${aManagementLocation.port}" // --management-port
        + s" ${aJubatusClusterConfiguration.resource.containerCount}" // --nodes
        + s" ${aJubatusClusterConfiguration.resource.priority}" // --priority
        + s" ${aJubatusClusterConfiguration.resource.memory}" // --memory
        + s" ${aJubatusClusterConfiguration.resource.virtualCores}" // --virtual-cores
        + s" ${aJubatusClusterConfiguration.resource.containerMemory}" // --container-memory
        + s" ${containerNodes}" // --container-nodes
        + s" ${containerRacks}" // --container-racks

        // ApplicationMaster, juba*_proxy, jubaconfig
        + s" ${aJubatusClusterConfiguration.learningMachineName}" // --learning-machine-name / --name
        + s" ${typeToString(aJubatusClusterConfiguration.learningMachineType)}" // --learning-machine-type / juba{}_proxy
        + s" ${aJubatusClusterConfiguration.zookeepers.map { z => s"${z.hostAddress}:${z.port}"}.mkString(",")}" // --zookeeper / --zookeeper

        // jubaconfig
        + s" $jubaConfigName" // -file

        + s" ${aJubatusClusterConfiguration.basePath}" // ApplicationMaster
        + s" ${aJubatusClusterConfiguration.serverConfig.thread}"  // --thread
        + s" ${aJubatusClusterConfiguration.serverConfig.timeout}"  // --timeout
        + s" ${aJubatusClusterConfiguration.serverConfig.mixer.name}"  // --mixer
        + s" ${aJubatusClusterConfiguration.serverConfig.intervalSec}"  // --interval_sec
        + s" ${aJubatusClusterConfiguration.serverConfig.intervalCount}"  // --interval_count
        + s" ${aJubatusClusterConfiguration.serverConfig.zookeeperTimeout}"  // --zookeeper_timeout
        + s" ${aJubatusClusterConfiguration.serverConfig.interconnectTimeout}"  // --interconnect_timeout

        // juba*_proxy
        + s" ${aJubatusClusterConfiguration.proxyConfig.thread}"  // --thread
        + s" ${aJubatusClusterConfiguration.proxyConfig.timeout}"  // --timeout
        + s" ${aJubatusClusterConfiguration.proxyConfig.zookeeperTimeout}"  // --zookeeper_timeout
        + s" ${aJubatusClusterConfiguration.proxyConfig.interconnectTimeout}"  // --interconnect_timeout
        + s" ${aJubatusClusterConfiguration.proxyConfig.poolExpire}"  // --pool_expire
        + s" ${aJubatusClusterConfiguration.proxyConfig.poolSize}"  // --pool_size

        // logconfig
        + s" ${applicationMasterLogConfigName}" //applicationMaster -> -Dlog4j.configuration
        + s" ${jubatusProxyLogConfigName}" // juba*_proxy -> --log_config
        + s" ${jubatusServerLogConfigPathString}"

        + s" 1>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stdout"
        + s" 2>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stderr"
      )
    tApplicationMasterContext.setCommands(List(tCommand).asJava)

    // リソース
    val tResource = Records.newRecord(classOf[YarnResource])
    tResource.setMemory(aJubatusClusterConfiguration.resource.proxyMemory + aJubatusClusterConfiguration.resource.masterMemory)
    tResource.setVirtualCores(aJubatusClusterConfiguration.resource.masterCores)

    // Application Master 登録
    val tApplication = mYarnClient.createApplication()
    val tContext = tApplication.getApplicationSubmissionContext
    tContext.setApplicationName(aJubatusClusterConfiguration.applicationName)
    tContext.setAMContainerSpec(tApplicationMasterContext)
    tContext.setResource(tResource)
    tContext.setQueue("default")

    logger.info(
      s"submit ApplicationMaster\n"
        + s"\tname: ${aJubatusClusterConfiguration.applicationName}\n"
        + s"\tcommand: $tCommand\n"
        + s"\tmemory: ${tResource.getMemory}\n"
        + s"\tvirtualCores: ${tResource.getVirtualCores}"
    )
    mYarnClient.submitApplication(tContext)
  }

  override def getStatus(aApplicationId: ApplicationId): ApplicationReport = mYarnClient.getApplicationReport(aApplicationId)

  override def getFinalStatus(aApplicationId: ApplicationId): FinalApplicationStatus = getStatus(aApplicationId).getFinalApplicationStatus

  override def kill(aApplicationId: ApplicationId): Unit = {
    logger.info(s"kill $aApplicationId")
    mYarnClient.killApplication(aApplicationId)
  }

  private def createConfigFile(aApplicationName: String, aConfigString: String, aBasePath: Path): Path = {
    val tHdfsPath = new Path(jubaConfigBasePath(aBasePath), s"${aApplicationName.replaceAll(":", "_")}.json")

    val tTempFile = File.createTempFile(s"jubatus-on-yarn-server", ".json")
    val tWriter = new PrintWriter(tTempFile)
    tWriter.println(aConfigString)
    tWriter.close()

    FileSystem.get(mYarnConfig).copyFromLocalFile(true, true, new Path(tTempFile.getPath), tHdfsPath)
    tHdfsPath
  }
}
