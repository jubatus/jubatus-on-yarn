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

import org.json4s._
import org.json4s.native.JsonMethods._
import us.jubat.yarn.common._

import scala.util.{Failure, Success, Try}
import org.apache.hadoop.fs.Path

case class ZooKeeperConverting(address: String, port: Int)

case class ApplicationMasterSettings(
                                      zookeepers: List[ZooKeeperConverting],
                                      learningMachineInstanceName: String,
                                      learningMachineType: LearningMachineType,
                                      nodeCount: Int,
                                      resources: Resource,
                                      configJson: String,
                                      basePath: String)

case class RegistrationParameters(name: String, address: InetAddress, port: Int)

/**
 * YarnClientに対するリクエストを処理します。
 */
class YarnClientServlet() extends RestServlet {

  private var mController: Option[YarnClientController] = None

  def setController(aController: YarnClientController): Unit = {
    mController = Some(aController)
  }

  put("/application/status") {
    logger.info(
      s"""put("/application/status") is called.
        |${request.body}
      """.stripMargin)
    Try {
      ControllerStatus.valueOf((parse(request.body) \ "status").extract[String]) match {
        case ControllerStatus.Stop =>
          mController.get.destroy()
        case tNewStatus =>
          mController.get.changeStatus(tNewStatus)
      }
    } match {
      case Success(msg) => //TODO: ログ出てないかも
        logger.info(msg.toString)
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }

  put("/application/location") {
    logger.info(
      s"""put("/application/location") is called.
        |${request.body}
      """.stripMargin)

    Try {
      val tJson = parse(request.body)
      val tApplicationMasterLocation = new Location(
        (tJson \ "applicationMaster" \ "host").extract[String],
        (tJson \ "applicationMaster" \ "port").extract[Int])

      val (tJubatusProxyLocation, tJubatusProxyType, tJubatusProxyName) = {
        val t = tJson \ "jubatusProxy" //TODO: 変数名
        (new Location((t \ "host").extract[String], (t \ "port").extract[Int]),
          LearningMachineType.valueOf((t \ "type").extract[String]),
          (t \ "name").extract[String])
      }

      mController.get.registerApplication(tApplicationMasterLocation, tJubatusProxyLocation, tJubatusProxyType, tJubatusProxyName)
    } match {
      case Success(msg) => //TODO: ログ
        logger.info(msg.toString)
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }

  put("/application/:seq/location") {
    logger.info(
      s"""put("/application/:seq=${params("seq")}/location") is called.
         |${request.body}""".stripMargin)
    Try {
      val tSeq = params("seq")
      val tJson = parse(request.body)

      val tContainerLocation = new Location(
        (tJson \ "container" \ "host").extract[String],
        (tJson \ "container" \ "port").extract[Int])
      val tJubatusServerLocation = new Location(
        (tJson \ "jubatusServer" \ "host").extract[String],
        (tJson \ "jubatusServer" \ "port").extract[Int])
      mController.get.registerContainer(tSeq.toInt, tContainerLocation, tJubatusServerLocation)
    } match {
      case Success(msg) => //TODO: ログ
        logger.info(msg.toString)
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }
}
