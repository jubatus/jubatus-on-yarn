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

import org.json4s._
import org.json4s.native.JsonMethods._
import us.jubat.yarn.common._

import scala.util.{Failure, Success, Try}

/**
 * Serverに対するリクエストを処理します。
 */
class ApplicationMasterServlet() extends RestServlet {

  private var mController: Option[ApplicationMasterController] = None

  def setController(aController: ApplicationMasterController) = {
    mController = Some(aController)
  }

  delete("/") {
    logger.info("""delete("/status") is called.""")
    Try {
      mController.get.stopSelf()
    } match {
      case Success(msg) => //TODO: stopSelf()など返り値がUnitのメソッドの場合msg.toStringはログ出してない可能性
        logger.info(msg.toString)
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }

  put("/:seq/status") {
    logger.info(
      s"""put("/:seq=${params("seq")}/status") is called.
        |${request.body}
      """.stripMargin)
    Try {
      val tSeq = params("seq").toInt
      (parse(request.body) \ "status").extract[String] match {
        case ControllerStatus.Wait.name =>
          logger.info("status is wait")
          mController.get.changeWait(tSeq)
        case ControllerStatus.Stop.name =>
          logger.info("status is stop")
          val tSeq = params("seq").toInt
          mController.get.removeContainer(tSeq)
      }
    } match {
      case Success(msg) =>
        logger.info(msg.toString) //TODO: ここもログでてないかも
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }

  put("/:seq/location") {
    logger.info(
      s"""put("/:seq=${params("seq")}/location") is called.
        |${request.body}
      """.stripMargin)
    Try {
      val tSeq = params("seq")
      val tParams = parse(request.body)
      val tContainerLocation = new Location(
        (tParams \ "container" \ "host").extract[String],
        (tParams \ "container" \ "port").extract[Int])
      val tJubatusLocation = new Location(
        (tParams \ "jubatusServer" \ "host").extract[String],
        (tParams \ "jubatusServer" \ "port").extract[Int])
      mController.get.registerContainer(tSeq.toInt, tContainerLocation, tJubatusLocation)
    } match {
      case Success(msg) =>
        logger.info(msg.toString)
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }

  post("/model/:id") {
    logger.info(
    s"""port("/model/:id=${params("id")}") is called.
       |${request.body}
     """.stripMargin
    )
    Try {
      val tId = params("id")
      val tParams = parse(request.body)
      val tPrefixPath = (tParams \ "pathPrefix").extract[String]
      mController.get.save(tPrefixPath, tId)
    } match {
      case Success(_) =>
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }

  put("/model") {
    logger.info(
      s"""put("/model") is called.
       |${request.body}
     """.stripMargin
    )
    Try {
      val tParams = parse(request.body)
      val tPrefixPath = (tParams \ "pathPrefix").extract[String]
      val tId = (tParams \ "id").extract[String]
      mController.get.load(tPrefixPath, tId)
    } match {
      case Success(_) =>
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }
}
