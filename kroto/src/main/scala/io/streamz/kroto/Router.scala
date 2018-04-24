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

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import io.streamz.kroto.impl.TopologyListener
import org.jgroups.JChannel

trait Router[A] extends AutoCloseable {
  def id: UUID
  def isLeader: Boolean
  def start(info: Endpoint): Unit
  def prefer(value: Int): Int
  def route(key: A): Option[Endpoint]
}

object Router {
  def apply[A](p: ProtocolInfo, t: Topology[A]): Router[A] =
    new Router[A] {
      private val routePreference = new AtomicInteger(100)
      private val jc = new JChannel(p():_*)

      def start(info: Endpoint): Unit = {
        jc.setReceiver(
          TopologyListener((e: TopologyEvent, eps: List[Endpoint]) => {

          }))
        jc.connect(p.clusterId)
        ()
      }

      def route(key: A): Option[Endpoint] = t.selectRoute(key)

      def isLeader: Boolean = false

      def prefer(value: Int): Int = {
        val res = routePreference.getAndSet(value)
        updateTopology()
        res
      }

      def close(): Unit = {
        jc.disconnect()
        ()
      }

      val id: UUID = UUID.randomUUID()

      private def updateTopology(): Unit = {
        val msg = t.toBytes
        t.endpoints.foreach { ep =>
          // TODO: try catch retry
          jc.send(ep.ip, msg)
        }
      }
    }

  def main(args: Array[String]): Unit = {

  }
}

