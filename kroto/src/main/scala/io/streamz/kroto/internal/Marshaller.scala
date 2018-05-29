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
package io.streamz.kroto.internal

import java.io.{DataInputStream, DataOutputStream, InputStream, OutputStream}
import java.net.URI

import io.streamz.kroto.{Endpoint, LogicalAddress, ReplicaSetId}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Marshaller {
  def write(epl: List[Set[Endpoint]], out: OutputStream) = {
    val os = new DataOutputStream(out)
    os.writeInt(epl.length)
    epl.foreach { set =>
      os.writeInt(set.size)
      set.foreach { ep =>
        os.writeUTF(ep.ep.toString)
        os.writeUTF(ep.id.value)
        os.writeUTF(ep.la.fold("")(_.value))
      }
    }
    os.flush()
  }

  def read(in: InputStream): List[Set[Endpoint]] = {
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
    listBuffer.toList
  }
}
