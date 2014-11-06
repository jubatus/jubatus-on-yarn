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

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.servlet.ServletHolder
import org.scalatra.ScalatraServlet

class JettyServer(aPort: Int, aContextPath: String, aServlet: ScalatraServlet) {
  def this(aContextPath: String, aServlet: ScalatraServlet) = this(0, aContextPath, aServlet)
  val mJettyServer = new Server(aPort)
  
  /**
   * Jetty server を起動します。
   */
  def start(): Unit = {
    val tContext = new WebAppContext()
    tContext.setContextPath(aContextPath)
    tContext.setResourceBase(".")
    tContext.addServlet(new ServletHolder(aServlet), "/*")

    mJettyServer.setHandler(tContext)
    mJettyServer.start()
  }

  /**
   * ポート番号を取得します。
   * [[JettyServer.start()]] が呼ばれている必要があります。
   * @return ポート番号
   */
  def getPort: Int = mJettyServer.getConnectors()(0).getLocalPort

  /**
   * Jetty server を停止します。
   */
  def stop(): Unit = mJettyServer.stop()
}
