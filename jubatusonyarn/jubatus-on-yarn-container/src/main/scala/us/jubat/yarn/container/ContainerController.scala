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

import java.io.File
import java.net.ConnectException
import scala.util.{Try, Success, Failure}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.yarn.conf.YarnConfiguration
import us.jubat.yarn.common.Predef._
import us.jubat.yarn.common._

import scala.util.Try


/**
 * Jubatusサーバに対応する監視、制御を行う。
 */
class ContainerController(name: String, seq: Int, applicationMasterLocation: Location, containerLocation: Location, jubatusConfig: JubatusConfig) extends HasLogger {

  private var mFinished = false

  /**
   * 終了ステータス。
   */
  def isFinished = mFinished

  private var mStatus: ControllerStatus = ControllerStatus.Init
  private val mApplicationMaster = new ApplicationMasterProxy(applicationMasterLocation, name, seq)
  mApplicationMaster.notifyLocation(containerLocation, jubatusConfig.location)
  mStatus = ControllerStatus.Wait

  private val mClient = Utils.createJubatusClient(jubatusConfig)

  {
    // 最大100ms * 300 = 30s待つ
    var tConnected = false
    var tLoopCount = 300
    while (tLoopCount > 0 && !tConnected) {
      try {
        mClient.getStatus
        tConnected = true
      } catch {
        case e: ConnectException =>
          tLoopCount -= 1
          Thread.sleep(100)
      }
    }
  }

  mApplicationMaster.notifyStatus(ControllerStatus.Wait)

  /**
   * JubatusServerにアクセスし、設定を取得します。
   * @return
   */
  private def getJubatusConfig: (String, String, String) = {
    Try {
      val tStatus = mClient.getStatus
      val tEntry = tStatus.entrySet().iterator().next()
      (tEntry.getKey, tEntry.getValue.get("datadir"), tEntry.getValue.get("type"))
    } match {
      case Success(t) =>
        logger.info("[SUCCESS] GET STATUS FROM JUBATUS_SERVER")
        t
      case Failure(e) =>
        logger.error("[FAILURE] GET STATUS FROM JUBATUS_SERVER", e)
        throw e
    }
  }

  /**
   * Jubatusが出力した保存モデルをHDFS上に格納します。
   * [[ContainerController.load()]]と対になります。
   *
   * @param aPathPrefix HDFS上の基点となるパス。
   * @param aId 保存名。
   */
  def save(aPathPrefix: String, aId: String): Unit = {

    requireState(mStatus == ControllerStatus.Wait)
    logger.info("ContainerController save")
    mStatus = ControllerStatus.Saving
    logger.info("start getJubatusConfig")
    val (tIpPort, tDir, tType) = getJubatusConfig
    logger.info("finish getJubatusConfig")

    val tLocalPathString = s"${tIpPort}_${tType}_$aId.jubatus"

    try {
      // CRCファイルが存在する場合は削除する。
      {
        val tCRC = new File(tDir, s".$tLocalPathString.crc")
        if(tCRC.exists()) tCRC.delete()
      }

      // ローカルファイルをコピー
      {
        val tHdfs = FileSystem.get(new YarnConfiguration())
        val tFromLocal = new Path(tDir, tLocalPathString)
        val tToHdfs = new Path(aPathPrefix, s"$aId/$seq.jubatus")
        tHdfs.copyFromLocalFile(true, true, tFromLocal, tToHdfs)
      }
    } finally {
      mStatus = ControllerStatus.Wait
      logger.debug(s"save: mStatus:$mStatus")
    }
  }

  /**
   * HDFS上に格納した保存モデルをJubatusに読みこませます。
   * [[ContainerController.save()]]と対になります。
   *
   * @param aPathPrefix HDFS上の基点となるパス。
   * @param aId 保存名。
   */
  def load(aPathPrefix: String, aId: String): Unit = {
    requireState(mStatus == ControllerStatus.Wait)
    mStatus = ControllerStatus.Loading
    try {
      val (tIpPort, tDir, tType) = getJubatusConfig
      FileSystem.get(new YarnConfiguration()).copyToLocalFile(false, new Path(aPathPrefix, s"$aId/$seq.jubatus"), new Path(tDir, s"${tIpPort}_${tType}_$aId.jubatus"))
    } finally {
      mStatus = ControllerStatus.Wait
      logger.debug(s"load: mStatus:$mStatus")
    }
  }

  /**
   * 監視しているJubatusServerを終了させます。
   */
  def stopSelf(): Unit = {
    // XXX 同期かどうか、強制化どうかもいるかも。
    logger.info(s"kill jubatus server($name:$seq, $jubatusConfig.processId).")
    Utils.kill(jubatusConfig.processId)

    // 自身の終了処理
    mStatus = ControllerStatus.Stop
    mApplicationMaster.notifyStatus(ControllerStatus.Stop)
    mFinished = true
  }
}
