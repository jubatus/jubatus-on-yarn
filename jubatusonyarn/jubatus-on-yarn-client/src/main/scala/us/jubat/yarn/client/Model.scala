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

import org.apache.hadoop.fs.Path
import java.net.InetAddress
import us.jubat.yarn.common.{Location, HasLogger, LearningMachineType}

import scala.util.{Failure, Success}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global


object Sample extends App with HasLogger {
  println("アプリケーションを起動します")

  val tApplicationFuture = JubatusYarnApplication.start(
    "shogun",
    LearningMachineType.Classifier,
    List(Location(InetAddress.getLocalHost, 2181)),
    new Path("hdfs:///jubatus-on-yarn/sample/shogun.json"),
    Resource(priority = 0, memory = 512, virtualCores = 1),
    1
  ).andThen {
    case Failure(e) =>
      println(e.getMessage)
      e.printStackTrace()
    case Success(tApplication) =>
      println(
        "アプリケーションが起動しました\n"
          + s"\t${tApplication.jubatusProxy}\n"
          + s"\t${tApplication.jubatusServers}"
      )

      println("アプリケーション1は30秒後に停止します")
      Thread.sleep(30 * 1000)

      println("アプリケーションを停止します")
      tApplication.stop()

      println("アプリケーションは停止しました")
  }
  Await.ready(tApplicationFuture, Duration.Inf)

  println("プログラムを終了します")
  System.exit(0)
}
