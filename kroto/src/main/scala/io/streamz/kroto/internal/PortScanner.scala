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

import java.net.{ServerSocket, Socket}

import scala.util.Random

object PortScanner {
  def scan(range: Range): Option[Int] = {
    val r = Random.shuffle(range.toList)
    def open(port: Int): Boolean = {
      try {
        val s = new ServerSocket(port)
        s.close()
        true
      }
      catch { case _: Throwable => false }
    }
    r.collectFirst { case port if open(port) => port }
  }

  def getFreePort: Option[Int] = {
    try {
      val s = new ServerSocket(0)
      val p = s.getLocalPort
      s.close()
      Some(p)
    }
    catch { case _: Throwable => None }
  }

  def isLocalPortInUse(port: Int) = {
    try {
      val s = new Socket("localhost", port)
      s.close()
      true
    }
    catch { case _: Throwable => false }
  }
}