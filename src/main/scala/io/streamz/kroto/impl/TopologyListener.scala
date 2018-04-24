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

import java.io.{OutputStream, InputStream}

import io.streamz.kroto.{TopologyEvent, Endpoint}
import org.jgroups.util.MessageBatch
import org.jgroups.{Address, View, Message, Receiver}

trait TopologyListener extends Receiver {

  override
  def suspect(suspected_mbr: Address): Unit = super.suspect(suspected_mbr)

  override
  def viewAccepted(new_view: View): Unit = super.viewAccepted(new_view)

  override
  def receive(batch: MessageBatch): Unit = super.receive(batch)

  override
  def getState(output: OutputStream): Unit = super.getState(output)

  override
  def setState(input: InputStream): Unit = super.setState(input)
}

object TopologyListener {
  def apply(f: (TopologyEvent, List[Endpoint]) => Unit) = new TopologyListener {
    def receive(msg: Message): Unit = {

    }
  }
}