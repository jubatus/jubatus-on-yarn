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

import dispatch.Defaults._
import dispatch.{Http, url, _}
import us.jubat.yarn.common.{ControllerStatus, HasLogger, Location, ParentProxy}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ApplicationMasterProxy(location: Location, name: String, seq: Int) extends ParentProxy(location) with HasLogger {
  private val mBaseUrl = s"http://${location.hostAddress}:${location.port}/v1/"

  def notifyLocation(containerLocation: Location, jubatusLocation: Location):Unit = {
    logger.info(s"notifyLocation() is called with these arguments: $containerLocation, $jubatusLocation and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "container":{
         |    "host":"${containerLocation.hostAddress}",
         |    "port":${containerLocation.port}
         |  },
         |  "jubatusServer":{
         |    "host":"${jubatusLocation.hostAddress}",
         |    "port":${jubatusLocation.port}
         |  }
         |}
       """.stripMargin
    val tRequest = (url(mBaseUrl) / seq / "location").PUT << tRequestBody //TODO: いらないと思うけど文字エンコードをヘッダで師弟する必要性はないか
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf) //TODO: ここの後ろでログとる必要性はないか
  }

  def notifyStatus(aStatus: ControllerStatus): Unit = {
    logger.info(s"notifyStatus() is called with this argument: $aStatus and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "status":"${aStatus.name}"
         |}
       """.stripMargin
    val tRequest = (url(mBaseUrl) / seq / "status").PUT << tRequestBody
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf)
  }
}

