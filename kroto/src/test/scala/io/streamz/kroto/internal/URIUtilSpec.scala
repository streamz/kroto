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

import org.specs2.mutable.Specification

class URIUtilSpec extends Specification {
  "A URI can be parsed" ! {
    val p0 = PortScanner.getFreePort.get
    val p1 = PortScanner.getFreePort.get
    val uri =
      new URI(s"tcp://localhost:$p0?node=localhost:$p0&node=localhost:$p1")
    val res = URIUtil.parseQuery(uri)
    res.size ==== 1
    res.get("node").fold(0)(_.size) ==== 2
  }
}
