package us.jubat.yarn.client

import org.scalatest._
import us.jubat.yarn.common.Location
import us.jubat.yarn.common.LearningMachineType
import org.apache.hadoop.fs.Path
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util._
import scala.concurrent.duration.Duration
import scala.sys.process.{Process, ProcessBuilder}
import org.apache.hadoop.conf.Configuration

class JubatusYarnApplicationSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val machineType = LearningMachineType.Classifier
  val zookeeper = new Location("localhost", 2181)
  val configString = """{"method":"AROW","parameter":{"regularization_weight":1.0},"converter":{"num_filter_types":{},"num_filter_rules":[],"string_filter_types":{},"string_filter_rules":[],"num_types":{},"num_rules":[{"key":"*","type":"num"}],"string_types":{"unigram":{"method":"ngram","char_num":"1"}},"string_rules":[{"key":"*","type":"unigram","sample_weight":"bin","global_weight":"bin"}]}}"""
  val basePath = new Path("hdfs:///jubatus-on-yarn/")
  val configPath = new Path("hdfs:///jubatus-on-yarn/test/jubatus_config.json")

  override def beforeAll(): Unit = {
    //テストデータの配置
    val conf = new Configuration()
    val fs = configPath.getFileSystem(conf)
    if (!fs.exists(configPath)) {
      val localPath = new Path("jubatus-on-yarn-client/src/test/resources/jubatus_config.json")
      fs.copyFromLocalFile(localPath, configPath)
    }
  }

  // start()結果からアプリケーション名を取得する
  private def getAppicationName(future: Future[JubatusYarnApplication]): String = {
    var applicationName = ""
    val result = future.andThen {
      case Success(j) =>
        applicationName = j.status.yarnApplication.getName
        j.kill()
      case Failure(t) =>
        print("CREATE MODEL failed: " + t.getMessage)
        t.printStackTrace()
    }
    Await.result(result, Duration.Inf)
    return applicationName
  }

  "start ()" should "check ApplicationName" in {
    //config parameter is String
    //specific paramter: No Path and No ApplicationName
    var future = JubatusYarnApplication.start("model1", machineType, List(zookeeper), configString, Resource(1, 1, 1), 3)
    var resultName = getAppicationName(future)
    resultName shouldBe "model1:" + machineType.name + ":" + zookeeper.hostAddress + ":" + zookeeper.port

    //specific paramter: Path and No ApplicationName
    future = JubatusYarnApplication.start("model2", machineType, List(zookeeper), configString, Resource(1, 1, 1), 3, basePath)
    resultName = getAppicationName(future)
    resultName shouldBe "model2:" + machineType.name + ":" + zookeeper.hostAddress + ":" + zookeeper.port

    //specific paramter: No Path and ApplicationName
    future = JubatusYarnApplication.start("model3", machineType, List(zookeeper), configString, Resource(1, 1, 1), 3, "dummyApplicationName3")
    resultName = getAppicationName(future)
    resultName shouldBe "dummyApplicationName3"

    //specific paramter: Path and ApplicationName
    future = JubatusYarnApplication.start("model4", machineType, List(zookeeper), configString, Resource(1, 1, 1), 3, basePath, "dummyApplicationName4")
    resultName = getAppicationName(future)
    resultName shouldBe "dummyApplicationName4"

    //config parameter is Path
    //specific paramter: No Path and No ApplicationName
    future = JubatusYarnApplication.start("model5", machineType, List(zookeeper), configPath, Resource(1, 1, 1), 3)
    resultName = getAppicationName(future)
    resultName shouldBe "model5:" + machineType.name + ":" + zookeeper.hostAddress + ":" + zookeeper.port

    //specific paramter: Path and No ApplicationName
    future = JubatusYarnApplication.start("model6", machineType, List(zookeeper), configPath, Resource(1, 1, 1), 3, basePath)
    resultName = getAppicationName(future)
    resultName shouldBe "model6:" + machineType.name + ":" + zookeeper.hostAddress + ":" + zookeeper.port

    //specific paramter: No Path and ApplicationName
    future = JubatusYarnApplication.start("model7", machineType, List(zookeeper), configPath, Resource(1, 1, 1), 3, "dummyApplicationName7")
    resultName = getAppicationName(future)
    resultName shouldBe "dummyApplicationName7"

    //specific paramter: Path and ApplicationName
    future = JubatusYarnApplication.start("model8", machineType, List(zookeeper), configPath, Resource(1, 1, 1), 3, basePath, "dummyApplicationName8")
    resultName = getAppicationName(future)
    resultName shouldBe "dummyApplicationName8"
  }

}
