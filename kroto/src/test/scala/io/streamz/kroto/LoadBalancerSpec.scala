/*
--------------------------------------------------------------------------------
    Copyright 2018 streamz.io
    KROTO: Klustered R0uting T0pology

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

import org.specs2.mutable.Specification

class LoadBalancerSpec extends Specification {
  val endpoint0 = Endpoint(
    new URI("http://r0.streamz.io"),
    ReplicaSetId("r0"),
    Some(LogicalAddress("1234")))
  val endpoint1 = Endpoint(
      new URI("http://r1.streamz.io"),
      ReplicaSetId("r1"),
      Some(LogicalAddress("5678")))

  "A random load balancer with a single endpoint returns the endpoint" ! {
    LoadBalancer.random(List(endpoint0))
      .fold(endpoint1)(identity) ==== endpoint0
  }

  "A random load balancer with a multiple endpoints returns an endpoint" ! {
    val res = LoadBalancer.random(List(endpoint0, endpoint1))
      .fold(endpoint1)(identity)
    (res == endpoint0 || res == endpoint1) ==== true
  }
}
