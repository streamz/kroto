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

import org.specs2.mutable.Specification

class XorShiftRngSpecSpec extends Specification {
  "generates a random integer with an even distribution" ! {
    val iterations = 1000000
    val seq = for (_ <- 0 to iterations) yield XorShiftRng.nextInt(100)
    val res = seq.groupBy(identity).mapValues(_.size)
    val mean = res.map(kv => kv._1 * kv._2).sum / iterations
    mean mustEqual 49
  }
}
