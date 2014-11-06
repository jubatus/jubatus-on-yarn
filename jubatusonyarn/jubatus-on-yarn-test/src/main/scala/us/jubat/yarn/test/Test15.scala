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

class TestTool2(aLearningMachineName: String, aLearningMachineType: LearningMachineType, aZookeepers: List[Location], aConfigFile: Path, aResource: Resource, aNodes: Int) {

  def run(): Unit = {
    println("アプリケーションを起動します")
    val tApplicationFuture = JubatusYarnApplication.start(aLearningMachineName, aLearningMachineType, aZookeepers, aConfigFile, aResource, aNodes, new Path("hdfs:///jubatus-on-yarn2")).andThen {
      case Failure(e) =>
        e.printStackTrace()

      case Success(tApplication) =>
        println(
          "アプリケーションが起動しました\n"
            + s"\tjubatusProxy: ${tApplication.jubatusProxy}\n"
            + s"\tjubatusServers: ${tApplication.jubatusServers}"
        )

        var tFinish = false
        while (!tFinish) {
          println("status/save/save2/load/stop/kill/exit")
          readLine("->") match {
            case "status" =>
              println("アプリケーションの状態を取得します")
              try {
                val tStatus = tApplication.status
                println(s"\tjubatusProxy: ${tStatus.jubatusProxy}")
                println(s"\tjubatusServers: ${tStatus.jubatusServers}")
                println(s"\tyarnApplication: ${tStatus.yarnApplication}")
              } catch {
                case e: Throwable => e.printStackTrace()
              }

            case "save" =>
              println("モデルデータを保存します")
              try {
                tApplication.saveModel(new Path("hdfs:///tmp/"), "test").get
                println("モデルデータを保存しました")
              } catch {
                case e: Throwable => e.printStackTrace()
              }

            case "save2" =>
              println("モデルデータを保存します")
              try {
                tApplication.saveModel(new Path("hdfs:///tmp/"), "test2").get
                println("モデルデータを保存しました")
              } catch {
                case e: Throwable => e.printStackTrace()
              }

            case "load" =>
              println("モデルデータを読み込みます")
              try {
                tApplication.loadModel(new Path("hdfs:///tmp/"), "test").get
                println("モデルデータを読み込みました")
              } catch {
                case e: Throwable => e.printStackTrace()
              }

            case "stop" =>
              try {
                println("アプリケーションを停止します")
                Await.ready(
                  tApplication.stop().andThen {
                    case Failure(e) => e.printStackTrace()
                    case _ => println("アプリケーションを停止しました")
                  },
                  Duration.Inf
                )
              } catch {
                case e: Throwable => e.printStackTrace()
              }

            case "kill" =>
              println("アプリケーションを強制終了します")
              try {
                tApplication.kill()
                println("アプリケーションを強制終了しました")
              } catch {
                case e: Throwable => e.printStackTrace()
              }

            case "exit" =>
              tFinish = true

            case _ =>
          }
        }
    }
    Await.ready(tApplicationFuture, Duration.Inf)
  }
}

// 中断
object Test15 extends App {

  val tTestTool = new TestTool2(
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
