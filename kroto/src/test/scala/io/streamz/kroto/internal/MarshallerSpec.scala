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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI

import io.streamz.kroto.{Endpoint, LogicalAddress, ReplicaSetId}
import org.specs2.mutable.Specification

class MarshallerSpec extends Specification {
  val endpoint0 = Endpoint(
    new URI("http://r0.streamz.io"),
    ReplicaSetId("r0"),
    Some(LogicalAddress("1234")))
  val endpoint1 = Endpoint(
    new URI("http://r1.streamz.io"),
    ReplicaSetId("r1"),
    Some(LogicalAddress("5678")))

  "A marshaller can marshall a list of sets" ! {
    val os = new ByteArrayOutputStream()
    Marshaller.write(List(Set(endpoint0, endpoint1)), os)
    val is = new ByteArrayInputStream(os.toByteArray)
    val res = Marshaller.read(is).flatMap(_.map(identity))
    os.close()
    is.close()
    res.contains(endpoint0) ==== true
    res.contains(endpoint1) ==== true
  }
}