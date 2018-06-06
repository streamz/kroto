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

import java.io.{DataInputStream, DataOutputStream, InputStream, OutputStream}
import java.net.URI

import io.streamz.kroto._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Marshaller {
  def write(state: TopologyState, out: OutputStream) = {
    writeReplicas(state.replicas, out)
    val os = new DataOutputStream(out)
    os.writeInt(state.eps.length)
    state.eps.foreach { set =>
      os.writeInt(set.size)
      set.foreach { ep =>
        os.writeUTF(ep.ep.toString)
        os.writeUTF(ep.id.value)
        os.writeUTF(ep.la.fold("")(_.value))
      }
    }
    os.flush()
  }

  def read(in: InputStream): TopologyState = {
    val replicas = readReplicas(in)
    val is = new DataInputStream(in)
    val listLen = is.readInt()
    val listBuffer = new ListBuffer[Set[Endpoint]]
    0 until listLen foreach { _ =>
      val setSize = is.readInt()
      val s = new mutable.HashSet[Endpoint]()
      0 until setSize foreach { _ =>
        val uri = new URI(is.readUTF())
        val rep = ReplicaSetId(is.readUTF())
        val la = is.readUTF()
        s.add(Endpoint(uri, rep, if (la.nonEmpty) Some(LogicalAddress(la)) else None))
      }
      listBuffer += s.toSet
    }
    TopologyState(listBuffer.toList, replicas)
  }

  def writeReplicas(state: ReplicaSets, out: OutputStream): Unit = {
    val os = new DataOutputStream(out)
    os.writeInt(state.value.size)
    state.value.foreach { t =>
      os.writeLong(t._1)
      os.writeUTF(t._2.value)
    }
  }

  def readReplicas(in: InputStream): ReplicaSets = {
    val is = new DataInputStream(in)
    val mapLen = is.readInt()
    ReplicaSets((0 until mapLen map { _ =>
      (is.readLong(), ReplicaSetId(is.readUTF()))
    }).toMap)
  }
}
