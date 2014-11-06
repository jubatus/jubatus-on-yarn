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

import us.jubat.yarn.common.{HasLogger, Location, JettyServer}
import java.net.InetAddress


class JubatusYarnService extends HasLogger {
  private var mYarnClientController: Option[YarnClientController] = None
  private var mJettyServer: Option[JettyServer] = None

  def yarnClientController: Option[YarnClientController] = mYarnClientController

  def start(): Unit = (mYarnClientController, mJettyServer) match {
    case (None, None) =>

      val tServlet = new YarnClientServlet
      val tJettyServer = new JettyServer("/v1", tServlet)
      tJettyServer.start()

      logger.info(s"started jetty server.(port=${tJettyServer.getPort}")

      val tController = new YarnClientController(Location(InetAddress.getLocalHost, tJettyServer.getPort))
      mYarnClientController = Some(tController)
      tServlet.setController(tController)

      mJettyServer = Some(tJettyServer)
    case _ => throw new IllegalStateException("Service is already running.")
  }

  def stop(): Unit = {
    (mYarnClientController, mJettyServer) match {
      case (Some(tYarnClientController), Some(tJettyServer)) => tJettyServer.stop()
      case _ => throw new IllegalStateException("Service is not running.")
    }
    mYarnClientController = None
    mJettyServer = None
  }
}
