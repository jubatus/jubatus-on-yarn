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
import us.jubat.yarn.common.{Location, LearningMachineType, ServerConfig, ProxyConfig, Mixer, LogConfig}
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

  it should "success log config" in {

    val yarnClient = new DefaultYarnClient()

    try {

      // 指定なし
      var jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "DefaultLogConfig", ServerConfig(), ProxyConfig(), None, basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      // ログ(submit ApplicationMaster)を目視確認
      // command: bash entrypoint.sh [ .... ] \"\" \"\" \"\" 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr.toString

      // yarn_am
      // 正常値指定あり
      val amlogConfig = LogConfig(Some("hdfs:///jubatus-on-yarn/test/am_log4j.xml"),None,None)
      jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "AmLogConfig", ServerConfig(), ProxyConfig(), Some(amlogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      // ログ(submit ApplicationMaster)を目視確認
      // command: bash entrypoint.sh [ .... ] applicationMasterLog4j.xml \"\" \"\" 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr.toString

      // jubatus_proxy
      // 正常値指定あり
      val proxylogConfig = LogConfig(None,Some("hdfs:///jubatus-on-yarn/test/jp_log4j.xml"),None)
      jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "JpLogConfig", ServerConfig(), ProxyConfig(), Some(proxylogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      // ログ(submit ApplicationMaster)を目視確認
      // command: bash entrypoint.sh [ .... ] \"\" jubatusProxyLog4j.xml \"\" 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr.toString

      // jubatus_server
      // 正常値指定あり
      val serverlogConfig = LogConfig(None, None, Some("hdfs:///jubatus-on-yarn/test/js_log4j.xml"))
      jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "JsLogConfig", ServerConfig(), ProxyConfig(), Some(serverlogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      // ログ(submit ApplicationMaster)を目視確認
      // command: bash entrypoint.sh [ .... ] \"\" \"\" hdfs:///jubatus-on-yarn/test/js_log4j.xml 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr.toString

      // ALL
      // 正常値指定あり
      val alllogConfig = LogConfig(Some("hdfs:///jubatus-on-yarn/test/am_log4j.xml"), Some("hdfs:///jubatus-on-yarn/test/jp_log4j.xml"), Some("hdfs:///jubatus-on-yarn/test/js_log4j.xml"))
      jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "AllLogConfig", ServerConfig(), ProxyConfig(), Some(alllogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      // ログ(submit ApplicationMaster)を目視確認
      // command: bash entrypoint.sh [ .... ] applicationMasterLog4j.xml jubatusProxyLog4j.xml hdfs:///jubatus-on-yarn/test/js_log4j.xml 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr.toString

    } catch {
      case e: Throwable =>
        e.printStackTrace()
        fail()
    }
  }

  it should "failed log config" in {
    val yarnClient = new DefaultYarnClient()

    // yarn_am
    // 値不正(file://)
    try {
      val amlogConfig = LogConfig(Some("file:///tmp/testConf/am_log4j.xml"), None, None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "AmLogConfig", ServerConfig(), ProxyConfig(), Some(amlogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      fail()
    } catch {
      case e: Exception =>
        e.getMessage() should include regex ".*Wrong FS.*"
    }
    // 値不正(スキームなし)
    try {
      val amlogConfig = LogConfig(Some("/jubatus-on-yarn/test/am_log4j.xml"), None, None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "AmLogConfig", ServerConfig(), ProxyConfig(), Some(amlogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      // ログ(submit ApplicationMaster)を目視確認
      // command: bash entrypoint.sh [ .... ] applicationMasterLog4j.xml \"\" \"\" 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr.toString
    } catch {
      case e: Exception =>
        e.printStackTrace()
        fail()
    }
    // 値不正(ファイルなし)
    try {
      val amlogConfig = LogConfig(Some("hdfs:///jubatus-on-yarn/test/noexist_log4j.xml"), None, None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "AmLogConfig", ServerConfig(), ProxyConfig(), Some(amlogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      fail()
    } catch {
      case e: Exception =>
        e.getMessage() should include regex ".*File does not exist.*"
    }

    // 値不正(ファイル形式不正)
    try {
      val amlogConfig = LogConfig(Some("hdfs:///jubatus-on-yarn/test/illegal_log4j.xml"), None, None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "AmLogConfig", ServerConfig(), ProxyConfig(), Some(amlogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
        // パラメータをログで目視確認
        // 1. テストケースの実行結果からapplicationIdを取得
        //      Submitted application application_XXXXXXXXXXXXX_YYYY
        // 2. yarn applicationログで確認
        //      yarn logs -applicationId application_XXXXXXXXXXXXX_YYYY
        // 3. ログに下記が出力されること
        //     org.xml.sax.SAXParseException; systemId: file:applicationMasterLog4j.xml [ .... ]
    } catch {
      case e: Exception =>
        e.printStackTrace()
        fail()
    }

    // jubatus_proxy
    // 値不正(file://)
    try {
      val jplogConfig = LogConfig(None, Some("file:///tmp/testConf/jp_log4j.xml"), None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "JpLogConfig", ServerConfig(), ProxyConfig(), Some(jplogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      fail()
    } catch {
      case e: Exception =>
        e.getMessage() should include regex ".*Wrong FS.*"
    }
    // 値不正(スキームなし)
    try {
      val jplogConfig = LogConfig(None, Some("/jubatus-on-yarn/test/jp_log4j.xml"), None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "JpLogConfig", ServerConfig(), ProxyConfig(), Some(jplogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      // ログ(submit ApplicationMaster)を目視確認
      // command: bash entrypoint.sh [ .... ] \"\" jubatusProxyLog4j.xml \"\" 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr.toString
    } catch {
      case e: Exception =>
        e.printStackTrace()
        fail()
    }
    // 値不正(ファイルなし)
    try {
      val jplogConfig = LogConfig(None, Some("hdfs:///jubatus-on-yarn/test/noexist_log4j.xml"), None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "JpLogConfig", ServerConfig(), ProxyConfig(), Some(jplogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
      fail()
    } catch {
      case e: Exception =>
        e.getMessage() should include regex ".*File does not exist.*"
    }

    // 値不正(ファイル形式不正)
    try {
      val jplogConfig = LogConfig(None, Some("hdfs:///jubatus-on-yarn/test/illegal_log4j.xml"), None)
      val jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "jpLogConfig", ServerConfig(), ProxyConfig(), Some(jplogConfig), basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)
        // パラメータをログで目視確認
        // 1. テストケースの実行結果からapplicationIdを取得
        //      Submitted application application_XXXXXXXXXXXXX_YYYY
        // 2. yarn applicationログで確認
        //      yarn logs -applicationId application_XXXXXXXXXXXXX_YYYY
        // 3. ログに下記が出力されること
        //     log4cxx: Error parsing file
    } catch {
      case e: Exception =>
        e.printStackTrace()
        fail()
    }
  }

  it should "success server config" in {

    val yarnClient = new DefaultYarnClient()

    try {
      // デフォルト
      var serverConfig = ServerConfig()
      var jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "TestApp1", serverConfig, ProxyConfig(), None, basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)

      // ログ(submit ApplicationMaster)を目視確認
      // command: bash [ .... ] 2 10 linear_mixer 16 512 10 10 [ proxy ] [stdout] [stderr]

      // 値指定あり
      serverConfig = ServerConfig(3, 30, Mixer.Random, 30, 1024, 15, 20)
      jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "TestApp1", serverConfig, ProxyConfig(), None, basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)

      // ログ(submit ApplicationMaster)を目視確認
      // command: bash [ .... ] 3 30 random_mixer 30 1024 15 20 [ proxy ] [stdout] [stderr]
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        fail()
    }
  }

  it should "success proxy config" in {

    val yarnClient = new DefaultYarnClient()

    try {
      // デフォルト
      var proxyConfig = ProxyConfig()
      var jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "TestApp1", ServerConfig(), proxyConfig, None, basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)

      // ログ(submit ApplicationMaster)を目視確認
      // command: bash [ .... ] 4 10 10 10 60 0  [stdout] [stderr]

      // 値指定あり
      proxyConfig = ProxyConfig(2, 20, 30, 40, 100, 16)
      jubaClusterConfig = JubatusClusterConfiguration("model1", LearningMachineType.Classifier, List(zookeeper),
        configString, None, Resource(), 2, "TestApp1", ServerConfig(), proxyConfig, None, basePath)
      yarnClient.submitApplicationMaster(jubaClusterConfig, manageLocation)

      // ログ(submit ApplicationMaster)を目視確認
      // command: bash [ .... ] 2 20 30 40 100 16  [stdout] [stderr]
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        fail()
    }
  }
}