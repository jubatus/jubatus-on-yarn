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

import java.util
import java.util.{Random, Collections}

import us.jubat.classifier.{EstimateResult, ClassifierClient, LabeledDatum}
import us.jubat.common.{ClientBase, Datum}
import us.jubat.yarn.common.{Location, LearningMachineType}
import us.jubat.yarn.client.{Resource, JubatusYarnApplication}
import java.net.InetAddress
import org.apache.hadoop.fs.Path
import scala.util.{Success, Failure}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

// priority
object Test10 extends App {
  def testcase(priority: Int): Unit = {
    println("アプリケーションを起動します")

    val tApplicationFuture = JubatusYarnApplication.start(
      "shogun",
      LearningMachineType.Classifier,
      List(Location(InetAddress.getLocalHost, 2181)),
      new Path(s"hdfs:///jubatus-on-yarn/sample/shogun.json"),
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

        println("アプリケーションの状態を取得します")
        val tStatus = tApplication.status
        println(s"\t${tStatus.jubatusProxy}")
        println(s"\t${tStatus.jubatusServers}")
        println(s"\t${tStatus.yarnApplication}")

        // train
        val tClient = new ClassifierClient(tApplication.jubatusProxy.hostAddress, tApplication.jubatusProxy.port, "shogun", 10)
        train(tClient)

        println("モデルデータを保存します")
        tApplication.saveModel(new Path("hdfs:///tmp/"), "test").get

        Thread.sleep(1000)

        println("モデルデータを読み込みます")
        tApplication.loadModel(new Path("hdfs:///tmp/"), "test").get

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
    Await.ready(tApplicationFuture, Duration.Inf)

    val tApplicationFuture2 = JubatusYarnApplication.start(
      "shogun2",
      LearningMachineType.Classifier,
      List(Location(InetAddress.getLocalHost, 2181)),
      new Path(s"hdfs:///jubatus-on-yarn/sample/shogun.json"),
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

        println("アプリケーションの状態を取得します")
        val tStatus = tApplication.status
        println(s"\t${tStatus.jubatusProxy}")
        println(s"\t${tStatus.jubatusServers}")
        println(s"\t${tStatus.yarnApplication}")

        println("モデルデータを読み込みます")
        tApplication.loadModel(new Path("hdfs:///tmp/"), "test").get

        // train
        val tClient = new ClassifierClient(tApplication.jubatusProxy.hostAddress, tApplication.jubatusProxy.port, "shogun2", 10)
        predict(tClient)

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
    Await.ready(tApplicationFuture2, Duration.Inf)
  }

  private def train(aClient: ClassifierClient): Unit = {
    def makeDatum(name: String): Datum = new Datum().addString("name", name)

    def makeTrain(tag: String, name: String): LabeledDatum = new LabeledDatum(tag, makeDatum(name))

    val trainData = scala.collection.mutable.ArrayBuffer (
      makeTrain("徳川", "家康"), makeTrain("徳川", "秀忠"), makeTrain("徳川", "家光"), makeTrain("徳川", "家綱"),
      makeTrain("徳川", "綱吉"), makeTrain("徳川", "家宣"), makeTrain("徳川", "家継"), makeTrain("徳川", "吉宗"),
      makeTrain("徳川", "家重"), makeTrain("徳川", "家治"), makeTrain("徳川", "家斉"), makeTrain("徳川", "家慶"),
      makeTrain("徳川", "家定"), makeTrain("徳川", "家茂"), //makeTrain("徳川", "慶喜"),

      makeTrain("足利", "尊氏"), makeTrain("足利", "義詮"), makeTrain("足利", "義満"), makeTrain("足利", "義持"),
      makeTrain("足利", "義量"), makeTrain("足利", "義教"), makeTrain("足利", "義勝"), makeTrain("足利", "義政"),
      makeTrain("足利", "義尚"), makeTrain("足利", "義稙"), makeTrain("足利", "義澄"), makeTrain("足利", "義稙"),
      makeTrain("足利", "義晴"), makeTrain("足利", "義輝"), makeTrain("足利", "義栄"), //makeTrain("足利", "義昭"),

      makeTrain("北条", "時政"), makeTrain("北条", "義時"), makeTrain("北条", "泰時"), makeTrain("北条", "経時"),
      makeTrain("北条", "時頼"), makeTrain("北条", "長時"), makeTrain("北条", "政村"), makeTrain("北条", "時宗"),
      makeTrain("北条", "貞時"), makeTrain("北条", "師時"), makeTrain("北条", "宗宣"), makeTrain("北条", "煕時"),
      makeTrain("北条", "基時"), makeTrain("北条", "高時"), makeTrain("北条", "貞顕") //, makeTrain("北条", "守時")
    )


    val t = trainData.asJava
    Collections.shuffle(t, new Random(0))

    // run train
    aClient.train(t)
  }

  private def predict(aClient: ClassifierClient): Unit = {
    def makeDatum(name: String): Datum = new Datum().addString("name", name)
    def findBestResult(res: List[EstimateResult]): Option[EstimateResult] = {
      res match {
        case List() => None
        case _ => Some(res.maxBy(_.score))
      }
    }

    // predict the last shogun
    val data = Array[Datum](makeDatum("慶喜"), makeDatum("義昭"), makeDatum("守時"))

    for (datum <- data) {
      val res = aClient.classify(util.Arrays.asList(datum))

      // get the predicted shogun name
      println(findBestResult(res.get(0).asScala.toList).get.label + datum.stringValues.get(0).value)
    }
  }

  println("==========================================================")
  println("No3")
  testcase(0)

  println("プログラムを終了します")
  System.exit(0)
}
