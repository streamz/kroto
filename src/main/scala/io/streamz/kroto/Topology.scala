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

package io.streamz.kroto

import java.util.UUID

import io.streamz.kroto.impl.{TopologyListener, TopologyEvent}
import org.jgroups.JChannel

trait Topology extends AutoCloseable {
  def id: UUID
  def isLeader: Boolean
  def start(info: NodeInfo): Unit
}

object Topology {
  def apply(p: ProtocolInfo, cb: (TopologyEvent, NodeInfo) => Unit): Topology =
    new Topology {
      private val jc = new JChannel(p():_*)

      def start(info: NodeInfo): Unit = {
        jc.setReceiver(
          TopologyListener((e: TopologyEvent, info: List[NodeInfo]) => {

          }))
        jc.connect(p.clusterId)
        ()
      }

      def isLeader: Boolean = ???

      def close(): Unit = {
        jc.disconnect()
        ()
      }

      val id: UUID = UUID.randomUUID()
    }
}