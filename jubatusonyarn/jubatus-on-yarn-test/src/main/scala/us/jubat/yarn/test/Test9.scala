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
package us.jubat.yarn.test

import us.jubat.yarn.common.{Location, LearningMachineType}
import us.jubat.yarn.client.{Resource, JubatusYarnApplication}
import java.net.InetAddress
import org.apache.hadoop.fs.Path
import scala.util.{Success, Failure}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

// 中断
object Test9 extends App {

  val tTestTool = new TestTool(
    "shogun",
    LearningMachineType.Classifier,
    List(Location(InetAddress.getLocalHost, 2181)),
    new Path(s"hdfs:///jubatus-on-yarn/sample/shogun.json"),
    Resource(priority = 0, memory = 512, virtualCores = 1),
    1
  )

  tTestTool.run()

  println("プログラムを終了します")
  System.exit(0)
}
