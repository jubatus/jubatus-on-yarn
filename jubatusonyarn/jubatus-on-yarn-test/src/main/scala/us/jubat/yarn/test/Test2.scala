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

import us.jubat.yarn.client.{Resource, JubatusYarnApplication}
import us.jubat.yarn.common.{Location, LearningMachineType}
import java.net.InetAddress
import org.apache.hadoop.fs.Path
import scala.util.{Success, Failure}
import us.jubat.classifier.ClassifierClient
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

// 並列 start -> save -> load -> stop できること
object Test2 extends App {

  def testcase(
                machineType1: LearningMachineType,
                machineName1: String,
                config1: Path,
                nodeCount1: Int,
                modelId1: String,

                machineType2: LearningMachineType,
                machineName2: String,
                config2: Path,
                nodeCount2: Int,
                modelId2: String

                ): Unit = {

    def applicationFuture(machineType: LearningMachineType, machineName: String, configPath: Path, nodeCount: Int, modelId: String): Future[JubatusYarnApplication] = JubatusYarnApplication.start(
      machineName,
      machineType,
      List(Location(InetAddress.getLocalHost, 2181)),
      configPath,
      Resource(priority = 0, memory = 512, virtualCores = 1),
      nodeCount
    ).andThen {
      case Failure(e) =>
        println(e.getMessage)
        e.printStackTrace()
      case Success(tApplication) =>
        try {
          println(
            "アプリケーションが起動しました\n"
              + s"\t${tApplication.jubatusProxy}\n"
              + s"\t${tApplication.jubatusServers}"
          )

          println("アプリケーションの状態を取得します")
          val tStatus = tApplication.status
          println(s"\t${tStatus.jubatusProxy}")
          println(s"\t${tStatus.jubatusServers}")
          println(s"\t${tStatus.yarnApplication}")

          println("モデルデータを保存します")
          tApplication.saveModel(new Path("hdfs:///tmp/"), modelId).get

          // Thread.sleep(1000)

          println("モデルデータを読み込みます")
          tApplication.loadModel(new Path("hdfs:///tmp/"), modelId).get
        } finally {
          println("アプリケーションを停止します")
          Await.ready(
            tApplication.stop().andThen {
              case Failure(e) =>
                println(e.getMessage)
                e.printStackTrace()
              case Success(_) =>
            },
            Duration.Inf
          )

          println("アプリケーションを停止しました")
        }
    }

    println("アプリケーションを起動します")
    val tApplication1 = applicationFuture(machineType1, machineName1, config1, nodeCount1, modelId1)
    println("アプリケーションを起動します")
    val tApplication2 = applicationFuture(machineType2, machineName2, config2, nodeCount2, modelId2)

    Await.ready(tApplication1, Duration.Inf)
    Await.ready(tApplication2, Duration.Inf)
    Thread.sleep(1000)
  }
  println("==========================================================")
  println("No1")
  testcase(
    LearningMachineType.Classifier, "shogun", new Path("hdfs:///jubatus-on-yarn/sample/shogun.json"), 3, "test1",
    LearningMachineType.Classifier, "gender", new Path("hdfs:///jubatus-on-yarn/sample/shogun.json"), 3, "test2"
  )
  println("==========================================================")
  println("No2")
  testcase(
    LearningMachineType.Classifier, "shogun", new Path("hdfs:///jubatus-on-yarn/sample/shogun.json"), 3, "test3",
    LearningMachineType.Recommender, "movielens", new Path("hdfs:///jubatus-on-yarn/sample/movielens.json"), 3, "test4"
  )


  println("プログラムを終了します")
  System.exit(0)
}
