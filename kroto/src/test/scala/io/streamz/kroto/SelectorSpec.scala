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

import java.net.URI
import io.streamz.kroto.internal.{Group, Marshaller, PortScanner}
import org.specs2.mutable.Specification


class SelectorSpec extends Specification {
  System.setProperty("java.net.preferIPv4Stack", "true")

  val uri0 = new URI("http://r0.streamz.io")
  val uri1 = new URI("http://r1.streamz.io")
  val endpoint0 = Endpoint(uri0, ReplicaSetId("r0"))
  val endpoint1 = Endpoint(uri1, ReplicaSetId("r0"))

  "A selector can select an endpoint" ! {
    val p0 = PortScanner.getFreePort.get
    val p1 = PortScanner.getFreePort.get
    val conf0 =
      new URI(s"tcp://localhost:$p0?node=localhost:$p0&node=localhost:$p1")
    val id = GroupId("group-test")
    val group0 = Group(conf0, id, top())

    val conf1 =
      new URI(s"tcp://localhost:$p1?node=localhost:$p0&node=localhost:$p1")
    val group1 = Group(conf1, id, top())

    val s0 = Selector(endpoint0, group0.get)
    val s1 = Selector(endpoint1, group1.get)

    s0.start()
    s1.start()

    Thread.sleep(2000)

    s0.select(0).fold(ReplicaSetId("r1"))(_.id) ==== endpoint0.id
    s0.close()

    s1.select(0).fold(uri0)(_.ep) ==== uri1
    s1.close()

    true ==== true
  }

  private def top() = {
    val t = Topology(
      Mapper.map(ReplicaSets(Map(0L -> endpoint0.id, 1L -> endpoint1.id)), identity[Long]),
      LoadBalancer.random,
      Marshaller.read,
      Marshaller.write
    )
    t
  }
}
