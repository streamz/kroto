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
package io.streamz.kroto.internal

import java.net.URI
import java.util.concurrent.atomic.AtomicReference

import io.streamz.kroto._
import org.specs2.mutable.Specification

class GroupSpec extends Specification {
  val endpoint0 = Endpoint(new URI("http://r0.streamz.io"), ReplicaSetId("r0"))
  val endpoint1 = Endpoint(new URI("http://r1.streamz.io"), ReplicaSetId("r1"))

  def top() = {
    val t = Topology(
      Mappers.mapped(new AtomicReference(Map[Int, ReplicaSetId](
        0 -> endpoint0.id,
        1 -> endpoint1.id
      ))),
      LoadBalancer.random,
      Marshaller.read,
      Marshaller.write
    )
    t
  }

  "A TCP group can be joined" ! {
    System.setProperty("java.net.preferIPv4Stack", "true")

    val p0 = PortScanner.getFreePort.get
    val p1 = PortScanner.getFreePort.get
    val uri0 = new URI(s"tcp://localhost:$p0?node=localhost:$p0&node=localhost:$p1")
    val id = GroupId("group-test")
    val group0 = Group(uri0, id, top())

    val uri1 = new URI(s"tcp://localhost:$p1?node=localhost:$p0&node=localhost:$p1")
    val group1 = Group(uri1, id, top())

    group0.get.join(endpoint0)
    group0.get.isLeader ==== true
    group1.get.join(endpoint1)
    group1.get.isLeader ==== false
    group0.get.leave()
    group1.get.leave()

    true ==== true
  }
}