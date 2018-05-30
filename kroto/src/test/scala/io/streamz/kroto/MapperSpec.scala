/*
--------------------------------------------------------------------------------
    Copyright 2018 streamz.io
    Cluster Hash Ring Router based on JGroups

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--------------------------------------------------------------------------------
*/
package io.streamz.kroto

import java.util.concurrent.atomic.AtomicReference

import org.specs2.mutable.Specification

class MapperSpec extends Specification {
  val rset = ReplicaSet(Map[Int, ReplicaSetId](
    0 -> ReplicaSetId("r0"),
    1 -> ReplicaSetId("r1"),
    2 -> ReplicaSetId("r2"),
    3 -> ReplicaSetId("r3"),
    4 -> ReplicaSetId("r4"),
    5 -> ReplicaSetId("r5"),
    6 -> ReplicaSetId("r6"),
    7 -> ReplicaSetId("r7"),
    8 -> ReplicaSetId("r8"),
    9 -> ReplicaSetId("r9")))

  "A mapper maps using a map" ! {
    Mappers.map(
      new AtomicReference[ReplicaSet[Int]](rset))(5)
        .fold(ReplicaSetId("r0"))(identity) ==== ReplicaSetId("r5")
  }

  "A mapper maps using a modulus" ! {
    Mappers.mod(rset, (a: String) => Math.abs(a.hashCode))("io.streamz")
      .fold(ReplicaSetId("r0"))(identity) ==== ReplicaSetId("r6")
  }

  "A mapper maps using a hash ring" ! {
    Mappers.ring(rset, identity[String])("io.streamz")
      .fold(ReplicaSetId("r1"))(identity) ==== ReplicaSetId("r0")
  }
}
