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
import us.jubat.yarn.common.{HasLogger, LearningMachineType, Location}

import scala.collection.JavaConverters._
import scala.io.Source

trait YarnClient {
  def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId

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
  private val jubaProxyMemory: Int = 32

  private def applicationMasterJarPath(aBasePath: Path): Path = new Path(aBasePath, "application-master/jubatus-on-yarn-application-master.jar")

  private val applicationMasterJarName: String = "jubatus-on-yarn-application-master.jar"
  private val applicationMasterMainClass: String = "us.jubat.yarn.applicationmaster.ApplicationMasterApp"
  private val applicationMasterMemory: Int = 128
  private val applicationMasterVirtualCores: Int = 1

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

  override def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId = {
    val tHdfsPath = new Path(jubaConfigBasePath(aBasePath), s"${aApplicationName.replaceAll(":", "_")}.json")

    val tTempFile = File.createTempFile(s"jubatus-on-yarn-server", ".json")
    val tWriter = new PrintWriter(tTempFile)
    tWriter.println(aConfigString)
    tWriter.close()

    FileSystem.get(mYarnConfig).copyFromLocalFile(true, true, new Path(tTempFile.getPath), tHdfsPath)

    submitApplicationMaster(aApplicationName, aLearningMachineInstanceName, aLearningMachineType, aZookeepers, tHdfsPath, aResource, aNodes, aManagementLocation, aBasePath)
  }


  override def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId = {
    logger.info(s"call submitApplicationMaster($aApplicationName, $aLearningMachineInstanceName, $aLearningMachineType, $aZookeepers, $aConfigFile, $aResource, $aNodes, $aManagementLocation)")
    val tApplicationMasterContext = Records.newRecord(classOf[ContainerLaunchContext])

    tApplicationMasterContext.setLocalResources(
      Map(
        entryScriptName -> toLocalResource(entryScriptPath(aBasePath)),
        jubaConfigName -> toLocalResource(aConfigFile),
        applicationMasterJarName -> toLocalResource(applicationMasterJarPath(aBasePath))
      ).asJava
    )

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
    val tCommand = (
      s"bash $entryScriptName"
        // ApplicationMaster の jar 起動用 java コマンド
        + s" $applicationMasterJarName"
        + s" $applicationMasterMainClass"
        + s" $applicationMasterMemory"

        // ApplicationMaster
        + s" $aApplicationName" // --application-name
        + s" ${aManagementLocation.hostAddress}" // --management-address
        + s" ${aManagementLocation.port}" // --management-port
        + s" $aNodes" // --nodes
        + s" ${aResource.priority}" // --priority
        + s" ${aResource.memory}" // --memory
        + s" ${aResource.virtualCores}" // --virtual-cores

        // ApplicationMaster, juba*_proxy, jubaconfig
        + s" $aLearningMachineInstanceName" // --learning-machine-name / --name
        + s" ${typeToString(aLearningMachineType)}" // --learning-machine-type / juba{}_proxy
        + s" ${aZookeepers.map { z => s"${z.hostAddress}:${z.port}"}.mkString(",")}" // --zookeeper / --zookeeper

        // jubaconfig
        + s" $jubaConfigName" // -file

        + s" $aBasePath" // ApplicationMaster

        + s" 1>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stdout"
        + s" 2>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stderr"
      )
    tApplicationMasterContext.setCommands(List(tCommand).asJava)

    // リソース
    val tResource = Records.newRecord(classOf[YarnResource])
    tResource.setMemory(jubaProxyMemory + applicationMasterMemory)
    tResource.setVirtualCores(applicationMasterVirtualCores)

    // Application Master 登録
    val tApplication = mYarnClient.createApplication()
    val tContext = tApplication.getApplicationSubmissionContext
    tContext.setApplicationName(aApplicationName)
    tContext.setAMContainerSpec(tApplicationMasterContext)
    tContext.setResource(tResource)
    tContext.setQueue("default")

    logger.info(
      s"submit ApplicationMaster\n"
        + s"\tname: $aApplicationName\n"
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
}
