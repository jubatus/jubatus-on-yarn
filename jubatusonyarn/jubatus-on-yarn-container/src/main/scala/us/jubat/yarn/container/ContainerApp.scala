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
package us.jubat.yarn.container

import org.kohsuke.args4j.CmdLineParser
import scala.collection.JavaConverters._
import us.jubat.yarn.common.{JubatusConfig, HasLogger, Location, JettyServer}
import java.net.InetAddress

object ContainerApp extends App with HasLogger {
  try {
    val tParams = new ContainerParams
    val tParser = new CmdLineParser(tParams)

    import scala.util.control.Exception._

    allCatch either tParser.parseArgument(args.toList.asJava) match {
      case Right(_) =>

        logger.info(s"starting container controller. (params=$tParams)")
        logger.info(s"application master port=${tParams.applicationMasterPort}")

        logger.info("started container controller.")
        logger.info("starting jetty server.")

        // REST Server 起動
        val tServlet = new ContainerServlet
        val tJettyServer = new JettyServer("/v1", tServlet)
        tJettyServer.start()

        logger.info(s"started jetty server.(port=${tJettyServer.getPort}")

        // Controller 起動
        val tController = new ContainerController(
          tParams.applicationName,
          tParams.seq,
          new Location(tParams.applicationMasterNodeAddress, tParams.applicationMasterPort),
          new Location(tParams.containerNodeAddress, tJettyServer.getPort),
          new JubatusConfig(
            aLearningMachineType = tParams.learningMachineType,
            aName = tParams.learningMachineName,
            aLocation = new Location(tParams.containerNodeAddress, tParams.jubatusServerPort),
            aTimeoutSec = 10,
            aProcessId = tParams.jubatusServerProcessId))


        tServlet.setController(tController)

        // 終了状態になるまで待機
        while (!tController.isFinished) {
          Thread.sleep(100)
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


class ContainerParams {
  @org.kohsuke.args4j.Option(name = "--application-name")
  var applicationName: String = ""

  @org.kohsuke.args4j.Option(name = "--seq")
  var seq: Int = 0

  @org.kohsuke.args4j.Option(name = "--application-master-address")
  var applicationMasterNodeAddress: String = ""

  @org.kohsuke.args4j.Option(name = "--application-master-port")
  var applicationMasterPort: Int = 0

  @org.kohsuke.args4j.Option(name = "--container-node-address")
  var containerNodeAddress: String = ""

  @org.kohsuke.args4j.Option(name = "--jubatus-server-port")
  var jubatusServerPort: Int = 0

  @org.kohsuke.args4j.Option(name = "--jubatus-server-process-id")
  var jubatusServerProcessId: Int = 0

  @org.kohsuke.args4j.Option(name = "--learning-machine-name")
  var learningMachineName: String = ""

  @org.kohsuke.args4j.Option(name = "--learning-machine-type")
  var learningMachineType: String = ""
}
