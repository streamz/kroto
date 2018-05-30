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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import org.specs2.mutable.Specification

class SPSCQueueSpec extends Specification {
  "A SPSCQueueSpec can submit and execute a task" ! {
    val l = new CountDownLatch(1)
    val b = new AtomicBoolean(false)
    val q = new SPSCQueue[() => Unit]((fn: () => Unit) => fn(), 10)
    q.push(() => {
      b.set(true)
      l.countDown()
    })
    l.await()
    b.get() ==== true
  }
}
