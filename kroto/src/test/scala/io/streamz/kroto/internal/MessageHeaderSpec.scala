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
package io.streamz.kroto.internal

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import io.streamz.kroto.{Hello, Sync}
import org.specs2.mutable.Specification

class MessageHeaderSpec extends Specification {
  "A MessageHeader can be serialized and de-serialized" ! {
    val hdr0 = new MessageHeader(Hello)
    val hdr1 = new MessageHeader()
    val ba = new ByteArrayOutputStream()
    val os = new DataOutputStream(ba)

    hdr0.writeTo(os)

    val is = new DataInputStream(new ByteArrayInputStream(ba.toByteArray))
    hdr1.readFrom(is)
    os.close()
    is.close()
    hdr1.toMsg.fold(Sync.id)(_.id) ==== Hello.id
  }
}
