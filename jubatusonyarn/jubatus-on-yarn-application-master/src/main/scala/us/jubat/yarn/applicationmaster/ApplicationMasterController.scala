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

import java.net.ConnectException

import us.jubat.yarn.common.Predef._
import us.jubat.yarn.common._

import scala.collection.mutable
import scala.util.{Failure, Success}

class ApplicationMasterController(name: String, nodeCount: Int, yarnLocation: Location, applicationMasterLocation: Location, jubatusConfig: JubatusConfig) extends HasLogger {

  @volatile
  private var mStatus: ControllerStatus = ControllerStatus.Init

  /**
   * 自身のステータス。
   */
  def status = mStatus

  private var mFinished = false

  /**
   * 終了ステータス。
   */
  def isFinished = mFinished

  /**
   * JubatusProxy。
   */
  private var mClient = Utils.createJubatusClient(jubatusConfig)

  {
    // 最大100ms * 300 = 30s待つ
    var tConnected = false
    var tLoopCount = 300
    while (tLoopCount > 0 && !tConnected) {
      try {
        mClient.getProxyStatus
        tConnected = true
      } catch {
        case e: ConnectException =>
          tLoopCount -= 1
          Thread.sleep(100)
      }
    }
  }

  /**
   * Yarn Client Proxy.
   */
  private val mYarnClient = new YarnClientProxy(yarnLocation, name)
  mYarnClient.notifyLocation(applicationMasterLocation, jubatusConfig)

  /**
   * Container Proxy.
   */
  private val mContainers = {
    val tContainers = mutable.Map.empty[Int, ContainerProxy]
    for (i <- 0 until nodeCount) {
      tContainers += i -> new ContainerProxy(name, i)
    }
    tContainers
  }

  def registerContainer(aSeq: Int, aContainerLocation: Location, aJubatusLocation: Location): Unit = {
    require(mContainers.size > aSeq, s"Unknown container seq(=$aSeq)")

    // update node info
    mContainers(aSeq).changeStarted(aContainerLocation, aJubatusLocation)
    mYarnClient.notifyContainerLocation(aSeq, aContainerLocation, aJubatusLocation)

    //    if(!mContainers.values.exists(_.status == ControllerStatus.Init)) {
    //      mYarnClient.registerContainers(mContainers.values)
    //    }
  }

  def changeWait(aSeq: Int): Unit = {
    mContainers.get(aSeq) match {
      case None => new IllegalArgumentException(s"Unknown container #$aSeq")
      case Some(tContainer) =>
        val tNeedStartUp = tContainer.status == ControllerStatus.Init
        tContainer.changeWait()
        if (tNeedStartUp && !mContainers.values.exists(_.status != ControllerStatus.Wait)) {
          mStatus = ControllerStatus.Wait
          mYarnClient.notifyStatus(ControllerStatus.Wait)
        }
    }
  }

  def save(aPrefixPath: String, aId: String): Unit = {
    requireState(status == ControllerStatus.Wait, s"Cannot save without ${ControllerStatus.Wait.name}.")
    mStatus = ControllerStatus.Saving
    try {
      mClient.save(aId)
      mContainers.values.foreach(_.save(aPrefixPath, aId))
    } finally {
      mStatus = ControllerStatus.Wait
    }
  }

  def load(aPrefixPath: String, aId: String): Unit = {
    requireState(status == ControllerStatus.Wait, s"Cannot load without ${ControllerStatus.Wait.name}.")
    mStatus = ControllerStatus.Loading
    try {
      mContainers.values.foreach(_.load(aPrefixPath, aId))
      mClient.load(aId)
    } finally {
      mStatus = ControllerStatus.Wait
    }
  }

  def killSelf(): Unit = {
    mStatus = ControllerStatus.Stop
    logger.info(s"kill jubatus proxy($name, ${jubatusConfig.processId}).")
    Utils.kill(jubatusConfig.processId)
  }

  def stopSelf(aForce: Boolean = false): Unit = {
    requireState(status == ControllerStatus.Wait, s"Cannot stop application master without ${ControllerStatus.Wait.name}.")

    // Forceフラグがない場合は事前に全てのノードをチェックする。
    if (!aForce) {
      for (c <- mContainers.values)
        requireState(c.status == ControllerStatus.Wait, s"Found node in doing something(=$name, ${c.seq}, ${c.status}})")
    }

    mStatus = ControllerStatus.Stop

    // XXX 同期かどうか、強制化どうかもいるかも。
    logger.info(s"kill jubatus proxy($name, ${jubatusConfig.processId}).")
    Utils.kill(jubatusConfig.processId)

    // Initの場合は、IP/Portすら存在しないので、停止要求が出せない。
    for (c <- mContainers.values if c.status != ControllerStatus.Init) {
      c.stop() match {
        case Success(_) =>
        case Failure(e) =>
          removeContainer(c.seq)
      }
    }
  }

  def removeContainer(aSeq: Int): Unit = {
    require(nodeCount > aSeq, s"Application master has $nodeCount containers, so not found #$aSeq container.")
    requireState(status == ControllerStatus.Stop, s"Cannot stop application master without ${ControllerStatus.Stop.name}.")

    mContainers -= aSeq

    if (mContainers.size == 0) {
      mYarnClient.notifyStatus(ControllerStatus.Stop)
      mFinished = true
    }
  }
}


