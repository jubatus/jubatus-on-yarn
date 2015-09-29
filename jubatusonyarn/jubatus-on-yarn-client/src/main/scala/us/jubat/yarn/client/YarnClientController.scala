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

import org.apache.hadoop.fs.Path
import org.apache.hadoop.yarn.api.records.{FinalApplicationStatus, ApplicationReport, ApplicationId}
import us.jubat.common.ClientBase
import us.jubat.yarn.common.Predef._
import us.jubat.yarn.common._
import scala.util.Try

/**
 * YarnClientレイヤーで処理の仲介を行うためのクラス。
 *
 * @param location 起動しているアドレス＆ポート。
 * @param yarnClient YarnServerとコンテナの起動通信を行う[[YarnClient]]
 */
class YarnClientController(location: Location, yarnClient: YarnClient = new DefaultYarnClient) extends HasLogger {

  private var mIsFinished = false

  def isFinished = mIsFinished

  private var mApplicationMaster: Option[ApplicationMasterProxy] = None
  private var mClient: Option[ClientBase] = None

  private def getFullName(aName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location]): String = {
    s"$aName:${aLearningMachineType.name}:${aZookeepers.map(z => z.hostAddress + ":" + z.port).mkString(",")}"
  }

  private def registerApplication(aFullName: String, aApplicationId: ApplicationId, aNodeCount: Int): ApplicationMasterProxy = {
    mApplicationMaster = Some(new ApplicationMasterProxy(aFullName, aApplicationId, aNodeCount))
    mApplicationMaster.get
  }

  def startJubatusApplication(aName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodeCount: Int, aBasePath: Path): ApplicationMasterProxy = {
    startJubatusApplication(aName, aLearningMachineType, aZookeepers, aConfigString, aResource, aNodeCount, aBasePath, null)
  }

  def startJubatusApplication(aName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodeCount: Int, aBasePath: Path, aApplicationName: String): ApplicationMasterProxy = {
    val tFullName = Option(aApplicationName).getOrElse(getFullName(aName, aLearningMachineType, aZookeepers))
    logger.info(s"starting $tFullName")

    val tApplicationId = yarnClient.submitApplicationMaster(tFullName, aName, aLearningMachineType, aZookeepers, aConfigString, aResource, aNodeCount, location, aBasePath)

    registerApplication(tFullName, tApplicationId, aNodeCount)
  }

  def startJubatusApplication(aName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodeCount: Int, aBasePath: Path): ApplicationMasterProxy = {
    startJubatusApplication(aName, aLearningMachineType, aZookeepers, aConfigFile, aResource, aNodeCount, aBasePath, null)
  }

  def startJubatusApplication(aName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodeCount: Int, aBasePath: Path, aApplicationName: String): ApplicationMasterProxy = {
    val tFullName = Option(aApplicationName).getOrElse(getFullName(aName, aLearningMachineType, aZookeepers))
    logger.info(s"starting $tFullName")

    val tApplicationId = yarnClient.submitApplicationMaster(tFullName, aName, aLearningMachineType, aZookeepers, aConfigFile, aResource, aNodeCount, location, aBasePath)

    registerApplication(tFullName, tApplicationId, aNodeCount)
  }

  /**
   * 新しい[[ApplicationMasterProxy]]を登録します。
   *
   * @param aApplicationMasterLocation ApplicationMasterのロケーション
   * @param aJubatusProxyLocation JubatusProxyのロケーション
   * @return
   */
  def registerApplication(aApplicationMasterLocation: Location, aJubatusProxyLocation: Location, aJubatusProxyType: LearningMachineType, aJubatusProxyName: String): Unit = {
    requireState(mApplicationMaster.isDefined)
    mClient = Some(Utils.createJubatusClient(new JubatusConfig(aJubatusProxyType, aJubatusProxyName, aJubatusProxyLocation, 10, 0)))
    mApplicationMaster.get.changeStarted(aApplicationMasterLocation, aJubatusProxyLocation)
  }

  def registerContainer(aSeq: Int, aContainerLocation: Location, aJubatusServerLocation: Location): Unit = {
    requireState(mApplicationMaster.isDefined)
    val tApp = mApplicationMaster.get
    require(aSeq < tApp.containers.size, s"Unknown container(=#$aSeq)")
    tApp.containers(aSeq).updateLocation(aContainerLocation, aJubatusServerLocation)
  }

  def getFinalStatus: FinalApplicationStatus = yarnClient.getFinalStatus(mApplicationMaster.get.applicationId)

  def status: JubatusYarnApplicationStatus = {
    requireState(mApplicationMaster.isDefined)
    requireState(mClient.isDefined)
    JubatusYarnApplicationStatus(
      jubatusProxy = mClient.get.getProxyStatus,
      jubatusServers = mClient.get.getStatus,
      yarnApplication = yarnClient.getStatus(mApplicationMaster.get.applicationId)
    )
  }

  def changeWait(): Unit = {
    requireState(mApplicationMaster.isDefined)
    mApplicationMaster.get.changeWait()
  }

  def changeStatus(aStatus: ControllerStatus): Unit = {
    requireState(mApplicationMaster.isDefined)
    mApplicationMaster.get.changeStatus(aStatus)
  }

  def saveModel(aModelPathPrefix: Path, aModelId: String): Unit = {
    requireState(mApplicationMaster.isDefined)
    mApplicationMaster.get.save(aModelPathPrefix.toString, aModelId)
  }

  def loadModel(aModelPathPrefix: Path, aModelId: String): Unit = {
    requireState(mApplicationMaster.isDefined)
    mApplicationMaster.get.load(aModelPathPrefix.toString, aModelId)
  }

  def stop(aForce: Boolean = false): Try[String] = {
    requireState(mApplicationMaster.isDefined)
    mApplicationMaster.get.stop()
  }

  def destroy(): Unit = {
    requireState(mApplicationMaster.isDefined)
    mApplicationMaster = None
    mClient = None
    mIsFinished = true
  }

  def kill(): Unit = {
    requireState(mApplicationMaster.isDefined)
    yarnClient.kill(mApplicationMaster.get.applicationId)
    destroy()
  }

}



