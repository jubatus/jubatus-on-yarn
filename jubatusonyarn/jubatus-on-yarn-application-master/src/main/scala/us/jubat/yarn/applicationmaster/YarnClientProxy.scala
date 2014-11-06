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

import dispatch.Defaults._
import dispatch._
import us.jubat.yarn.common._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class YarnClientProxy(val location: Location, val name: String) extends ParentProxy(location) with HasLogger {

  val mBaseUrl = s"http://${location.hostAddress}:${location.port}/v1/"

  def notifyLocation(aApplicationMasterLocation: Location, aJubatusProxyConfig: JubatusConfig): Unit = {
    logger.info(s"notifyLocation() is called with these arguments: $aApplicationMasterLocation, $aJubatusProxyConfig and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "applicationMaster":{
         |    "host":"${aApplicationMasterLocation.hostAddress}",
         |    "port":${aApplicationMasterLocation.port}
         |  },
         |  "jubatusProxy":{
         |    "host":"${aJubatusProxyConfig.location.hostAddress}",
         |    "port":${aJubatusProxyConfig.location.port},
         |    "type":"${aJubatusProxyConfig.learningMachineType.name}",
         |    "name":"${aJubatusProxyConfig.name}"
         |  }
         |}
       """.stripMargin
    val tRequest = (url(mBaseUrl) / "application" / "location").PUT << tRequestBody
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf)
  }

  def notifyContainerLocation(aSeq: Int, aContainerLocation: Location, aJubatusServerLocation: Location): Unit = {
    logger.info(s"notifyContainerLocation() is called with these arguments: $aSeq, $aContainerLocation, $aJubatusServerLocation and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "container":{
         |    "host":"${aContainerLocation.hostAddress}",
         |    "port":${aContainerLocation.port}
         |  },
         |  "jubatusServer":{
         |    "host":"${aJubatusServerLocation.hostAddress}",
         |    "port":${aJubatusServerLocation.port}
         |  }
         |}
       """.stripMargin
    val tRequest = (url(mBaseUrl) / "application" / aSeq / "location").PUT << tRequestBody
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf)
  }

  override def notifyStatus(aStatus: ControllerStatus): Unit = {
    logger.info(s"notifyStatus() is called with this argument: $aStatus and waits infinitely until response is back.")
    val tRequestBody =
      s"""{
         |  "status":"${aStatus.name}"
         |}
       """.stripMargin
    val tRequest = (url(mBaseUrl) / "application" / "status").PUT << tRequestBody
    val tResponse = Http(tRequest OK as.String)
    Await.result(tResponse, Duration.Inf)
  }
}
