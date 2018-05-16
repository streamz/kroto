/*
--------------------------------------------------------------------------------
    Copyright 2018 streamz.io
    KROTO: Klustering ROuter TOpology

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

import io.streamz.kroto.impl.XorShiftRng

trait LoadBalancer {
  def select(endpoints: List[Endpoint]): Option[Endpoint]
}

object LoadBalancer {
  def random = (endpoints: List[Endpoint]) =>
    endpoints.size match {
      case 0 => None
      case 1 => endpoints.headOption
      case n: Int =>
        Some(endpoints(XorShiftRng.nextInt(100) % n))
    }
}
