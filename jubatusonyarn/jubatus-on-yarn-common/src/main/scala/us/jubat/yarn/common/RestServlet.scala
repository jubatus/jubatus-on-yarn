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

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet

/**
 * Created by vagrant on 14/10/25.
 */
abstract class RestServlet extends ScalatraServlet with HasLogger {
  protected implicit def jsonFormats: Formats = DefaultFormats

  before() {
    contentType = "application/json; charset=utf-8" //formats("json")
  }

}
