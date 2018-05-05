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
package io.streamz.kroto.impl

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport

class SPSCQueue[A](fn: A => Unit, depth: Int) extends AutoCloseable {
  private val queue = new ArrayBlockingQueue[A](depth)
  private val running = new AtomicBoolean(true)
  private val thread = new Thread(new Runnable {
    override def run() = {
      while (running.get()) {
        val a = queue.poll()
        if (a != null) fn(a)
        LockSupport.parkNanos(1)
      }
    }
  })

  thread.setDaemon(true)
  thread.start()

  def push(a: A): Boolean = queue.add(a)
  def close() = running.set(false)
}
