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

import org.scalatest._
import java.net.{InetSocketAddress, InetAddress}
import us.jubat.yarn.common._
import org.apache.hadoop.fs.{Path, FileSystem}
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.eclipse.jetty.server.Server
import scala.util.{Failure, Success, Try}

class ContainerControllerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  private var Controller: ContainerController = null
  private var tAmJettyServer: JettyServer = null
  private var tCnJettyServer: JettyServer = null
  private var dmyJubaServer: DummyJubatusServer = null

  override def beforeAll(): Unit = {
    dmyJubaServer = new DummyJubatusServer
    dmyJubaServer.start(9300)

    val dmyAmServlet = new DummyApplicationMasterServlet
    tAmJettyServer = new JettyServer("/v1", dmyAmServlet)
    tAmJettyServer.start()

    val tDmyCnServlet = new ContainerServlet
    tCnJettyServer = new JettyServer("/v1", tDmyCnServlet)
    tCnJettyServer.start()

    val amLocation = new Location(InetAddress.getLocalHost, tAmJettyServer.getPort)
    val cnLocation = new Location(InetAddress.getLocalHost, tCnJettyServer.getPort)
    val location = new Location(InetAddress.getLocalHost, 9300)
    val tJubatusConfig = new JubatusConfig(LearningMachineType.Classifier, "test", location, 5, 999)
    Controller = new ContainerController("test", 0, amLocation, cnLocation, tJubatusConfig)
  }

  override protected def afterAll(): Unit = {
    dmyJubaServer.stop()
    tAmJettyServer.stop()
    tCnJettyServer.stop()
  }

  "save()" should "save success" in {
    val sFile = new java.io.File("/tmp/192.168.122.231_9300_classifier_test001.jubatus")
    if (!sFile.exists()) {
      sFile.createNewFile()
    }

    val tHdfs = FileSystem.get(new YarnConfiguration())
    val tToHdfs = new Path("hdfs:///data/models/t1/test001/0.jubatus")
    if (tHdfs.exists(tToHdfs)) {
      tHdfs.delete(tToHdfs, false)
    }

    try {
      Controller.save("hdfs:///data/models/t1", "test001")
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        fail()
    }

    tHdfs.exists(tToHdfs) shouldBe true
  }

  it should "save error exception" in {
    val sFile = new java.io.File("/tmp/192.168.122.231_9300_classifier_test001.jubatus")
    if (sFile.exists()) {
      sFile.delete()
    }

    val tHdfs = FileSystem.get(new YarnConfiguration())
    val tToHdfs = new Path("hdfs:///data/models/t1/test001/0.jubatus")
    if (tHdfs.exists(tToHdfs)) {
      tHdfs.delete(tToHdfs, false)
    }

    try {
      Controller.save("hdfs:///data/models/t1", "test001")
    } catch {
      case e: Throwable =>
        e.printStackTrace()
    }

    tHdfs.exists(tToHdfs) shouldBe false
  }
}

class DummyApplicationMasterServlet() extends RestServlet {

  put("/:seq/status") {
    logger.info(
      s"""put("/:seq=${params("seq")}/status") is called.
        |${request.body}
      """.stripMargin)
    "OK"
  }

  put("/:seq/location") {
    logger.info(
      s"""put("/:seq=${params("seq")}/location") is called.
        |${request.body}
      """.stripMargin)
    "OK"
  }
}

class DummyJubatusServer {
  var server: org.msgpack.rpc.Server = null

  class JubaServer {
    def get_status(): java.util.Map[String, java.util.Map[String, String]] = {
      var ret: java.util.Map[String, java.util.Map[String, String]] = new java.util.HashMap()
      var ret2: java.util.Map[String, String] = new java.util.HashMap()
      ret2.put("datadir", "file:///tmp")
      ret2.put("type", "classifier")
      ret.put("192.168.122.231_9300", ret2)
      ret
    }

  }

  def start(id: Int) {
    server = new org.msgpack.rpc.Server()
    server.serve(new JubaServer())
    server.listen(new InetSocketAddress(id))
    println("*** DummyJubatusServer start ***")
  }

  def stop() {
    server.close()
    println("*** DummyJubatusServer stop ***")
  }
}

