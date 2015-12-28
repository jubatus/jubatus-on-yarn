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
package us.jubat.yarn.applicationmaster

import org.kohsuke.args4j.CmdLineParser
import us.jubat.yarn.common._

import scala.collection.JavaConverters._
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus

object ApplicationMasterApp extends App with HasLogger {
  try {
    val tParams = new ApplicationMasterParams
    val tParser = new CmdLineParser(tParams)

    import scala.util.control.Exception._

    //    val t = "--application-name shogun:Classifier:localhost.localdomain/127.0.0.1:2181 --nodes 1 --priority 0 --memory 1024 --virtual-cores 1 --learning-machine-name shogun --learning-machine-type classifier --zookeeper 127.0.0.1:2181 --management-address 127.0.0.1 --management-port 8080 --application-master-node-address 192.168.10.103 --application-master-port 9181 --jubatus-proxy-port 9180 --jubatus-proxy-process-id 4952".split(" ")
    //    allCatch either tParser.parseArgument(t.toList.asJava) match {
    allCatch either tParser.parseArgument(args.toList.asJava) match {
      case Right(_) =>

        logger.info(s"starting application master controller. (params = $tParams)")

        logger.info(s"started application master controller.")
        logger.info(s"starting jetty server.")

        // REST Server 起動
        val tServlet = new ApplicationMasterServlet
        val tJettyServer = new JettyServer("/v1", tServlet)
        tJettyServer.start()

        logger.info(s"started jetty server.(port=${tJettyServer.getPort}")

        // Controller 起動
        val tController = {
          val tName = tParams.applicationName
          val tNodeCount = tParams.nodes
          val tYarnConfig: Location = new Location(tParams.managementAddress, tParams.managementPort)
          val tAppLocation: Location = new Location(tParams.applicationMasterNodeAddress, tJettyServer.getPort)
          val tJubatusConfig: JubatusConfig = new JubatusConfig(
            tParams.learningMachineType,
            tParams.learningMachineName,
            new Location(tParams.applicationMasterNodeAddress, tParams.jubatusProxyPort),
            10, // TODO 外部から取得できるように
            tParams.jubatusProxyProcessId
          )

          new ApplicationMasterController(tName, tNodeCount, tYarnConfig, tAppLocation, tJubatusConfig)
        }

        tServlet.setController(tController)

        logger.info("starting application master.")

        // AM 起動
        val tApplicationMaster = new ApplicationMaster()
        tApplicationMaster.run(tParams, tJettyServer.getPort) match {
          case FinalApplicationStatus.SUCCEEDED =>
            // 終了状態になるまで待機
            while (!tController.isFinished) {
              Thread.sleep(100)
            }
          case FinalApplicationStatus.FAILED =>
            // コンテナ側で異常発生したら自ら終了する
            tController.killSelf()
        }

        logger.info("stopping jetty server.")

        // REST Server 停止
        tJettyServer.stop()

        logger.info("stopped jetty server.")

      case Left(e) =>
        logger.error(s"parse parameters error. ", e)
        tParser.printSingleLineUsage(System.out)
    }

    System.exit(0)

  } catch {
    case e: Throwable =>
      logger.error("unknown error happen.", e)
      System.exit(1)
  }
}

class ApplicationMasterParams {
  @org.kohsuke.args4j.Option(name = "--application-name")
  var applicationName: String = ""


  @org.kohsuke.args4j.Option(name = "--nodes")
  var nodes: Int = 1


  @org.kohsuke.args4j.Option(name = "--priority")
  var priority: Int = 0

  @org.kohsuke.args4j.Option(name = "--memory")
  var memory: Int = 128

  @org.kohsuke.args4j.Option(name = "--virtual-cores")
  var virtualCores: Int = 1

  @org.kohsuke.args4j.Option(name = "--container-memory")
  var containerMemory: Int = 128

  @org.kohsuke.args4j.Option(name = "--container-nodes")
  var containerNodes: String = ""

  @org.kohsuke.args4j.Option(name = "--container-racks")
  var containerRacks: String = ""


  @org.kohsuke.args4j.Option(name = "--learning-machine-name")
  var learningMachineName: String = ""

  @org.kohsuke.args4j.Option(name = "--learning-machine-type")
  var learningMachineType: String = ""

  @org.kohsuke.args4j.Option(name = "--zookeeper")
  var zooKeepers: String = ""


  @org.kohsuke.args4j.Option(name = "--management-address")
  var managementAddress: String = ""

  @org.kohsuke.args4j.Option(name = "--management-port")
  var managementPort: Int = 0


  @org.kohsuke.args4j.Option(name = "--application-master-node-address")
  var applicationMasterNodeAddress: String = ""

  @org.kohsuke.args4j.Option(name = "--jubatus-proxy-port")
  var jubatusProxyPort: Int = 0

  @org.kohsuke.args4j.Option(name = "--jubatus-proxy-process-id")
  var jubatusProxyProcessId: Int = 0


  @org.kohsuke.args4j.Option(name = "--base-path")
  var basePath: String = ""

  override def toString(): String = {
    val text = s"""applicationName: $applicationName, nodes: $nodes, priority: $priority,
      memory: $memory, virtualCores: $virtualCores, containerMemory: $containerMemory,
      containerNodes: $containerNodes, containerRacks: $containerRacks, learningMachineName: $learningMachineName,
      learningMachineType: $learningMachineType, zooKeepers: $zooKeepers, managementAddress: $managementAddress,
      managementPort: $managementPort, applicationMasterNodeAddress: $applicationMasterNodeAddress,
      jubatusProxyPort: $jubatusProxyPort, jubatusProxyProcessId: $jubatusProxyProcessId
      """.stripMargin.trim
    text
  }
}
