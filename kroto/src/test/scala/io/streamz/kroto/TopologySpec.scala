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
import java.util.concurrent.atomic.AtomicReference

import io.streamz.kroto.internal.Marshaller
import org.specs2.mutable.Specification

class TopologySpec extends Specification {
  val endpoint0 = Endpoint(
    new URI("http://r0.streamz.io"),
    ReplicaSetId("r0"),
    Some(LogicalAddress("1234")))
  val endpoint1 = Endpoint(
    new URI("http://r1.streamz.io"),
    ReplicaSetId("r1"),
    Some(LogicalAddress("5678")))
  val endpoint2 = Endpoint(
    new URI("http://r2.streamz.io"),
    ReplicaSetId("r2"),
    Some(LogicalAddress("9101")))
  val endpoint3 = Endpoint(
    new URI("http://r3.streamz.io"),
    ReplicaSetId("r3"),
    Some(LogicalAddress("1112")))

  val top = Topology(
    Mappers.mapped(
      new AtomicReference(
        Map[Int, ReplicaSetId](
        0 -> ReplicaSetId("r0"),
        1 -> ReplicaSetId("r1"),
        2 -> ReplicaSetId("r2"),
        3 -> ReplicaSetId("r3")))),
    LoadBalancer.random,
    Marshaller.read,
    Marshaller.write
  )

  top.add(endpoint0)
  top.add(endpoint1)
  top.add(endpoint2)
  top.add(endpoint3)

  "A topology manages connected nodes" ! {
    top.select(0).fold(endpoint1)(identity) ==== endpoint0
    top.select(1).fold(endpoint1)(identity) ==== endpoint1
    top.select(2).fold(endpoint1)(identity) ==== endpoint2
    top.select(3).fold(endpoint1)(identity) ==== endpoint3
    top.find(LogicalAddress("1234")).fold(endpoint1)(identity) ==== endpoint0
    top.find(LogicalAddress("5678")).fold(endpoint1)(identity) ==== endpoint1
    top.find(LogicalAddress("9101")).fold(endpoint1)(identity) ==== endpoint2
    top.find(LogicalAddress("1112")).fold(endpoint1)(identity) ==== endpoint3
  }
}