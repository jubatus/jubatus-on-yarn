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

import dispatch.{Http, url, as}
import us.jubat.yarn.common.Predef._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
 * 子ノード(Application Master, Container)の共通プロキシ.
 */
abstract class ChildProxy extends HasLogger {

private lazy val mBaseUrl = url(s"http://${containerLocation.get.hostAddress}:${containerLocation.get.port}/v1/")
private var mStatus: ControllerStatus = ControllerStatus.Init
  private var mContainerLocation: Option[Location] = None
  private var mJubatusLocation: Option[Location] = None

  def containerLocation = mContainerLocation

  def jubatusLocation = mJubatusLocation

  def status = mStatus

  def changeStarted(aContainerLocation: Location, aJubatusLocation: Location): Unit = {
    requireState(status == ControllerStatus.Init, s"Already started ${mContainerLocation.get.hostAddress}:${mContainerLocation.get.port}")
    mContainerLocation = Some(aContainerLocation)
    mJubatusLocation = Some(aJubatusLocation)
  }

  def changeWait(): Unit = {
    changeStatus(ControllerStatus.Wait)
  }
  
  def changeStatus(aNewStatus: ControllerStatus) : Unit = {
    aNewStatus match {
      case ControllerStatus.Wait =>
        requireState(status == ControllerStatus.Init
          || status == ControllerStatus.Saving
          || status == ControllerStatus.Loading, s"Cannot change to wait from this status(${mStatus.name})")
      case ControllerStatus.Saving | ControllerStatus.Loading | ControllerStatus.Stop =>
        requireState(status == ControllerStatus.Wait)
      case ControllerStatus.Init =>
        requireState(false)
    }
    mStatus = aNewStatus
  }

  def save(aModelPathPrefix: String, aModelId: String): Unit = {
    requireState(status == ControllerStatus.Wait, s"Cannot stop application master without ${ControllerStatus.Wait.name}.")
    changeStatus(ControllerStatus.Saving)
    try {
      requestSave(aModelPathPrefix, aModelId)
    } finally {
      changeStatus(ControllerStatus.Wait)
    }
  }

  protected def requestSave(aModelPathPrefix: String, aModelId: String): Unit = {
    logger.info("requestSave() is called and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "pathPrefix":"$aModelPathPrefix"
         |}
       """.stripMargin
    val tRequest = (mBaseUrl / "model" / aModelId).POST << tRequestBody
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf)
  }

  def load(aModelPathPrefix: String, aModelId: String): Unit = {
    requireState(status == ControllerStatus.Wait, s"Cannot load in this status(${mStatus.name}")
    changeStatus(ControllerStatus.Loading)
    try {
      requestLoad(aModelPathPrefix, aModelId)
    } finally {
      changeStatus(ControllerStatus.Wait)
    }
  }

  protected def requestLoad(aModelPathPrefix: String, aModelId: String): Unit = {
    logger.info("requestLoad() is called and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "pathPrefix":"$aModelPathPrefix"
         |  "id":"$aModelId"
         |}
       """.stripMargin
    val tRequest = (mBaseUrl / "model").PUT << tRequestBody
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf)
  }

  def stop(): Try[String] = {
    requireState(status == ControllerStatus.Wait, s"Cannot stop in this status(${mStatus.name}")
    changeStatus(ControllerStatus.Stop)
    requestStop()
  }

  protected def requestStop(): Try[String] = Try {
    logger.info("requestStop() is called and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "status":"${ControllerStatus.Stop.name}"
         |}
       """.stripMargin
    val tRequest = mBaseUrl.DELETE << tRequestBody
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf)
  }
}

