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

import us.jubat.yarn.common.LearningMachineType
import org.apache.hadoop.fs.Path

import org.scalatest._

class JubatusClusterConfigurationSpec extends FlatSpec with Matchers {
  "valid configuration" should "pass validation" in {
    noException should be thrownBy {
      new JubatusClusterConfiguration(
        "valid",
        LearningMachineType.Classifier,
        "localhost:2181",
        Some(""),
        None,
        new Resource(0, 64, 1),
        2,
        None,
        None
      ).validate()
    }
  }

  "invalid configuration" should "fail validation" in {
    an[IllegalArgumentException] should be thrownBy {
      new JubatusClusterConfiguration(
        "", /* <= ERROR: empty config */
        LearningMachineType.Classifier,
        "localhost:2181",
        Some(""),
        None,
        new Resource(0, 64, 1),
        2,
        None,
        None
      ).validate()
    }

    an[IllegalArgumentException] should be thrownBy {
      new JubatusClusterConfiguration(
        "model",
        LearningMachineType.Classifier,
        "", /* <= ERROR: empty ZK */
        Some(""),
        None,
        new Resource(0, 64, 1),
        2,
        None,
        None
      ).validate()
    }

    an[IllegalArgumentException] should be thrownBy {
      new JubatusClusterConfiguration(
        "model",
        LearningMachineType.Classifier,
        "localhost:2181",
        None, /* <= ERROR */
        None, /* <= ERROR: both config file and config path are not specified */
        new Resource(0, 64, 1),
        2,
        None,
        None
      ).validate()
    }

    an[IllegalArgumentException] should be thrownBy {
      new JubatusClusterConfiguration(
        "model",
        LearningMachineType.Classifier,
        "localhost:2181",
        Some("{}"), /* <= ERROR */
        Some(new Path("file:///")), /* <= ERROR: both config file and config path are specified  */
        new Resource(0, 64, 1),
        2,
        None,
        None
      ).validate()
    }

    an[IllegalArgumentException] should be thrownBy {
      new JubatusClusterConfiguration(
        "model",
        LearningMachineType.Classifier,
        "localhost:2181",
        Some("{}"),
        None,
        new Resource(0, 0 /* <= ERROR: invalid amount of memory */ , 1),
        2,
        None,
        None
      ).validate()
    }

    an[IllegalArgumentException] should be thrownBy {
      new JubatusClusterConfiguration(
        "model",
        LearningMachineType.Classifier,
        "localhost:2181",
        Some("{}"),
        None,
        new Resource(0, 1, 0 /* <= ERROR: invalid number of cores */),
        2,
        None,
        None
      ).validate()
    }

    an[IllegalArgumentException] should be thrownBy {
      new JubatusClusterConfiguration(
        "model",
        LearningMachineType.Classifier,
        "localhost:2181",
        Some("{}"),
        None,
        new Resource(0, 64, 1),
        0, /* <= ERROR: invalid number of nodes */
        None,
        None
      ).validate()
    }
  }
}
