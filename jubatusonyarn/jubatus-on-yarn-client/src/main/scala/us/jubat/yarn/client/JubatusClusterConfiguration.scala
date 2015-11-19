// Jubatus: Online machine learning framework for distributed environment
// Copyright (C) 2015 Preferred Networks and Nippon Telegraph and Telephone Corporation.
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

import org.apache.hadoop.fs.Path
import us.jubat.yarn.common.LearningMachineType

case class Resource(priority: Int, memory: Int, virtualCores: Int)

case class JubatusClusterConfiguration(
                                        learningMachineName: String,
                                        learningMachineType: LearningMachineType,
                                        zookeeper: String,
                                        configString: Option[String],
                                        configFile: Option[Path],
                                        resource: Resource,
                                        nodeCount: Int,
                                        applicationName: Option[String],
                                        basePath: Option[Path]
                                        ) {
  def validate(): this.type = {
    require(this.learningMachineName.nonEmpty, "learningMachineName must not be empty")
    require(this.zookeeper.nonEmpty, "zookeeper must not be empty")
    require(this.configString.isDefined || this.configFile.isDefined, "configString or configFile must be specified")
    require(!(this.configString.isDefined && this.configFile.isDefined), "configString and configFile cannot be specified at the same time")
    require(this.resource.memory > 0, "memory must be at least 1 MB")
    require(this.resource.virtualCores > 0, "virtualCores must be at least 1")
    require(this.nodeCount > 0, "nodeCount must be more than 1")
    this
  }
}
