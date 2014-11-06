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
package us.jubat.yarn.container

import org.json4s._
import org.json4s.native.JsonMethods._
import us.jubat.yarn.common.{ControllerStatus, RestServlet}

import scala.util.{Failure, Success, Try}

case class ClientParameters(applicationId: String, path: String)

/**
 * Clientに対するリクエストを処理します。
 */
class ContainerServlet extends RestServlet {

  private var mController: Option[ContainerController] = None

  def setController(aController: ContainerController): Unit = {
    mController = Some(aController)
  }

  //モデルを保存します。
  post("/model/:id") {
    logger.info(
      s"""post("/model/:id${params("id")}") is called.
         |${request.body}
       """.stripMargin)
    Try {
      val tId = params("id")
      val tParams = parse(request.body)
      val tPathPrefix = (tParams \ "pathPrefix").extract[String]

      mController.get.save(tPathPrefix, tId)
      """{"status": "ok"}"""
    } match {
      case Success(msg) =>
        logger.info(msg)
        msg
      case Failure(e) =>
        logger.info("error happen. ", e)
        halt(400, "request must contains parameter 'path'.")
    }
  }

  //モデルを更新します。
  put("/model") {
    logger.info(
      s"""put("/model") is called.
         |${request.body}
       """.stripMargin)
    Try {
      val tParams = parse(request.body)
      val tId = (tParams \ "id").extract[String]
      val tPathPrefix = (tParams \ "pathPrefix").extract[String]

      mController.get.load(tPathPrefix, tId)
      """{"status": "ok"}"""
    } match {
      case Success(msg) =>
        logger.info(msg)
        msg
      case Failure(e) =>
        logger.info("error happen", e)
        halt(400, "request must contains parameter 'path'.")
    }
  }

  put("/status") {
    logger.info(
      """put("/status") is called.
        |${request.body}
      """.stripMargin)
    Try {
      (parse(request.body) \ "status").extract[String] match {
        case ControllerStatus.Stop.name =>
          mController.get.stopSelf()
      }
    } match {
      case Success(msg) =>
        logger.info(msg.toString) //TODO: ログ
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }

  delete("/") {
    logger.info("""delete("/") is called.""")
    Try {
      mController.get.stopSelf()
    } match {
      case Success(msg) =>
        logger.info(msg.toString) //TODO: ログ
        msg
      case Failure(e) =>
        logger.info("error happen.", e)
        halt(400, e)
    }
  }
}
