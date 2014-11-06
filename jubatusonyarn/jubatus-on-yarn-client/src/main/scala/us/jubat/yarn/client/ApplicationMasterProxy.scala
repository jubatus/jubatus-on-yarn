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

import org.apache.hadoop.yarn.api.records.ApplicationId
import us.jubat.yarn.common.{ChildProxy, HasLogger, Location}

import scala.collection.mutable

class ContainerProxy {
  private var mContainerLocation: Option[Location] = None
  private var mJubatusLocation: Option[Location] = None

  def updateLocation(aContainerLocation: Location, aJubatusLocation: Location): Unit = {
    mContainerLocation = Some(aContainerLocation)
    mJubatusLocation = Some(aJubatusLocation)
  }

  def containerLocation = mContainerLocation

  def jubatusLocation = mJubatusLocation
}

class ApplicationMasterProxy(val name: String, val applicationId: ApplicationId, nodeCount: Int) extends ChildProxy with HasLogger {

  val containers = {
    val tContainers = mutable.Map.empty[Int, ContainerProxy]
    for (i <- 0 until nodeCount) tContainers += i -> new ContainerProxy
    tContainers
  }
}
