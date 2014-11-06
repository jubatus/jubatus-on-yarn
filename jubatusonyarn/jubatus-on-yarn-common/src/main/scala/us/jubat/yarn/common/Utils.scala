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
package us.jubat.yarn.common

import java.net.InetAddress

import org.slf4j.LoggerFactory
import us.jubat.anomaly.AnomalyClient
import us.jubat.classifier.ClassifierClient
import us.jubat.common.ClientBase
import us.jubat.recommender.RecommenderClient

trait HasLogger {
  val logger = LoggerFactory.getLogger(getClass)
}

case class Location(host: InetAddress, port: Int) {
  def this(aHostName: String, aPort: Int) = this(InetAddress.getByName(aHostName), aPort)

  //  def hostName = host.getHostName
  def hostAddress = host.getHostAddress
}

object ControllerStatus {

  case object Init extends ControllerStatus("Init")

  case object Wait extends ControllerStatus("Wait")

  case object Saving extends ControllerStatus("Saving")

  case object Loading extends ControllerStatus("Loading")

  case object Stop extends ControllerStatus("Stop")

  def valueOf(aValue: String): ControllerStatus = {
    aValue match {
      case ControllerStatus.Init.name => ControllerStatus.Init
      case ControllerStatus.Wait.name => ControllerStatus.Wait
      case ControllerStatus.Saving.name => ControllerStatus.Saving
      case ControllerStatus.Loading.name => ControllerStatus.Loading
      case ControllerStatus.Stop.name => ControllerStatus.Stop
    }
  }
}


sealed abstract class ControllerStatus(aName: String) {
  val name = aName
}

sealed abstract class LearningMachineType(val name: String)

object LearningMachineType {

  case object Anomaly extends LearningMachineType("anomaly")

  case object Classifier extends LearningMachineType("classifier")

  case object Recommender extends LearningMachineType("recommender")

  def valueOf(aValue: String): LearningMachineType = {
    aValue match {
      case Anomaly.name => Anomaly
      case Classifier.name => Classifier
      case Recommender.name => Recommender
    }
  }
}

case class JubatusConfig(learningMachineType: LearningMachineType, name: String, location: Location, timeoutSec: Int, processId: Int) {
  def this(aLearningMachineType: String, aName: String, aLocation: Location, aTimeoutSec: Int, aProcessId: Int)
  = this(LearningMachineType.valueOf(aLearningMachineType), aName, aLocation, aTimeoutSec, aProcessId)
}

object Utils {
  def kill(aProcessId: Int, aNeedWaitFor: Boolean = true): Unit = {
    val tProcess = Runtime.getRuntime.exec("kill " + aProcessId)
    if (aNeedWaitFor) tProcess.waitFor()
  }

  def createJubatusClient(aConfig: JubatusConfig): ClientBase = {
    aConfig.learningMachineType match {
      case LearningMachineType.Anomaly => new AnomalyClient(aConfig.location.hostAddress, aConfig.location.port, aConfig.name, aConfig.timeoutSec)
      case LearningMachineType.Classifier => new ClassifierClient(aConfig.location.hostAddress, aConfig.location.port, aConfig.name, aConfig.timeoutSec)
      case LearningMachineType.Recommender => new RecommenderClient(aConfig.location.hostAddress, aConfig.location.port, aConfig.name, aConfig.timeoutSec)
    }
  }
}

object Predef extends scala.LowPriorityImplicits {

  def requireState(aRequirement: scala.Boolean): scala.Unit = requireState(aRequirement, "")

  @scala.inline
  final def requireState(aRequirement: scala.Boolean, aMessage: => scala.Any): scala.Unit = {
    if (!aRequirement) throw new IllegalStateException(aMessage.toString)
  }
}
