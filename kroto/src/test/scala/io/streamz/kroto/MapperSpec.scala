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

import org.specs2.mutable.Specification

class MapperSpec extends Specification {
  val rset = ReplicaSets(Map[Long, ReplicaSetId](
    0L -> ReplicaSetId("r0"),
    1L -> ReplicaSetId("r1"),
    2L -> ReplicaSetId("r2"),
    3L -> ReplicaSetId("r3"),
    4L -> ReplicaSetId("r4"),
    5L -> ReplicaSetId("r5"),
    6L -> ReplicaSetId("r6"),
    7L -> ReplicaSetId("r7"),
    8L -> ReplicaSetId("r8"),
    9L -> ReplicaSetId("r9")))

  "A mapper merges ReplicaSets" ! {
    val m = Mapper.map(rset, identity[Long])
    m.merge(ReplicaSets(Map(9L->ReplicaSetId("r10"), 11L->ReplicaSetId("r11"))))
    m(11L).fold(ReplicaSetId("r0"))(identity) ==== ReplicaSetId("r11")
    m(9L).fold(ReplicaSetId("r0"))(identity) ==== ReplicaSetId("r10")
  }

  "A mapper maps using a map" ! {
    Mapper.map(rset, identity[Long])(5L)
        .fold(ReplicaSetId("r0"))(identity) ==== ReplicaSetId("r5")
  }

  "A mapper maps using a modulus" ! {
    Mapper.mod(rset, (a: String) => Math.abs(a.hashCode))("io.streamz")
      .fold(ReplicaSetId("r0"))(identity) ==== ReplicaSetId("r6")
  }

  "A mapper maps using a hash ring" ! {
    Mapper.ring(rset, identity[String])("io.streamz")
      .fold(ReplicaSetId("r1"))(identity) ==== ReplicaSetId("r0")
  }
}
