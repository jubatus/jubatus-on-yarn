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

import java.net.InetAddress
import org.apache.hadoop.fs.Path
import org.apache.hadoop.yarn.api.records.{FinalApplicationStatus, ApplicationReport, ApplicationId}
import org.scalatest._
import us.jubat.yarn.common.{LearningMachineType, Location}

class YarnClientControllerSpec extends FlatSpec with Matchers with BeforeAndAfter {

  class DummyYarnClient extends YarnClient {

    override def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigString: String, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId = {
      ApplicationId.newInstance(0, 0)
    }

    override def submitApplicationMaster(aApplicationName: String, aLearningMachineInstanceName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodes: Int, aManagementLocation: Location, aBasePath: Path): ApplicationId = {
      ApplicationId.newInstance(0, 0)
    }

    override def getStatus(aApplicationId: ApplicationId): ApplicationReport = ???

    override def kill(aApplicationId: ApplicationId): Unit = ???

    override def getFinalStatus(aApplicationId: ApplicationId): FinalApplicationStatus = ???
  }

  def createController() = new YarnClientController(Location(InetAddress.getLocalHost, 0), new DummyYarnClient())

  "startJubatusApplication ()" should "check applicationName" in {

    val machineType = LearningMachineType.Classifier
    val zookeeper1 = new Location("localhost",2188)
    val zookeeper2 = new Location("127.0.0.2",2189)

    val tController = createController()

    var result = tController.startJubatusApplication("model1", machineType, List(zookeeper1,zookeeper2), "configString", Resource(0, 0, 0), 3, null)
    result.name shouldBe "model1:" + machineType.name + ":" + zookeeper1.hostAddress + ":" + zookeeper1.port + "," + zookeeper2.hostAddress + ":" + zookeeper2.port

    result = tController.startJubatusApplication("model2", machineType, List(zookeeper1,zookeeper2), "configString", Resource(0, 0, 0), 3, null, "dummyApplicationName2")
    result.name shouldBe "dummyApplicationName2"

    result = tController.startJubatusApplication("model3", machineType, List(zookeeper1,zookeeper2), new Path("/tmp/dummyFile"), Resource(0, 0, 0), 3, null)
    result.name shouldBe "model3:" + machineType.name + ":" + zookeeper1.hostAddress + ":" + zookeeper1.port + "," + zookeeper2.hostAddress + ":" + zookeeper2.port

    result = tController.startJubatusApplication("model4", machineType, List(zookeeper1,zookeeper2), new Path("/tmp/dummyFile"), Resource(0, 0, 0), 3, null, "dummyApplicationName4")
    result.name shouldBe "dummyApplicationName4"
  }

//  "start one" should "not throw exception" in {
//    val tController = createController()
//    tController.startJubatusApplication("applicationName", LearningMachineType.Classifier, List.empty[Location], "configString", Resource(0, 0, 0), 3)
//  }

//  "start two with same name" should "throw exception" in {
//    val tController = createController()
//    tController.startJubatusApplication("applicationName", LearningMachineType.Classifier, List.empty[Location], "configString", Resource(0, 0, 0), 3)
//
//    intercept[IllegalArgumentException] {
//      tController.startJubatusApplication("applicationName", LearningMachineType.Classifier, List.empty[Location], "configString", Resource(0, 0, 0), 3)
//    }
//  }


  // TODO ここから後はREST飛ばしてエラーになるので、ちょっと後回し。
  // "normal case"
//  ignore should "not throw exception" in {
//    val tAppName = "applicationName"
//    val tNodeCount = 3
//
//    // start
//    val tController = createController()
//    val tApp = tController.startJubatusApplication(tAppName, LearningMachineType.Classifier, List.empty[Location], "configString", Resource(0, 0, 0), tNodeCount)
//
//    // register
//    tController.registerApplication(new Location("1.1.1.1", 100), new Location("1.1.1.2", 200))
//    for (i <- 0 until tNodeCount)
//      tController.registerContainer(i, new Location(s"1.1.2.$i", 100), new Location(s"1.1.3.$i", 200))
//
//    // started
//    tController.changeWait()
//
//    // stop
//    tController.stop()
//
//    // remove
//    tController.removeApplication(tApp.name)
//  }
}
