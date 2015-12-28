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

import org.scalatest._
import us.jubat.yarn.common.{Location, LearningMachineType}
import org.apache.hadoop.fs.Path

class YarnClientSpec extends FlatSpec with Matchers {
  val zookeeper = new Location("localhost", 2181)
  val configString = """{"method":"AROW","parameter":{"regularization_weight":1.0}}"""
  val manageLocation = new Location("localhost", 9300)
  val basePath = new Path("hdfs:///jubatus-on-yarn")

  "submitApplicationMaster ()" should "success recource config" in {

    val yarnClient = new DefaultYarnClient()

    try {
      // デフォルト
      var resource = Resource()
      yarnClient.submitApplicationMaster("TestApp1", "model1", LearningMachineType.Classifier, List(zookeeper),
          configString, resource, 3, manageLocation, basePath)

      // ログ(submit ApplicationMaster)を目視確認
      // command: bash [entrypoint] [jar] [class] 128 [ap] [host] [port] [nodes] 0 256 1 128 \"\" \"\" [model] [type] [zookeeper] [config] [basepath] [stdout] [stderr]
      // memory: 32+128 = 160
      // virtualCores: 1

      resource = Resource(1, 512, 2, 256, 128, 3, 256, List("1"), List("1", "2"))
      yarnClient.submitApplicationMaster("TestApp1", "model1", LearningMachineType.Classifier, List(zookeeper),
          configString, resource, 3, manageLocation, basePath)

      // ログ(submit ApplicationMaster)を目視確認
      // command: bash [entrypoint] [jar] [class] 256 [ap] [host] [port] [nodes] 1 512 2 256 "1" "1,2" [model] [type] [zookeeper] [config] [basepath] [stdout] [stderr]
      // memory: 128+256 = 384
      // virtualCores: 3
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        fail()
    }
  }
}