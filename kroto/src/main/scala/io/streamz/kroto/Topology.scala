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

import java.io.{InputStream, OutputStream}
import java.util.concurrent.atomic.AtomicReference
import io.streamz.kroto.impl.HashRing
import scala.collection.{mutable => m}

trait Topology {
  /**
    * Select an endpoint
    * @param key the routing key
    * @return the optional endpoint
    */
  def select(key: String): Option[Endpoint]

  def read(in: InputStream): Unit
  def write(out: OutputStream): Unit

  private [kroto] def message(msg: Msg): Unit
  private [kroto] def add(ep: Endpoint)
  private [kroto] def remove(ep: Endpoint)
}

/**
  * Topology maintains a cluster of multiple ReplicaSets. ReplicaSets
  * can service the same set of keys, determined by the keyFn. Individual
  * endpoint selection is determined by the balance function.
  */
object Topology {
  def apply(
    balance: m.Set[Endpoint] => Option[Endpoint],
    reader: InputStream => List[Set[Endpoint]],
    writer: (List[Set[Endpoint]], OutputStream) => Unit) = new Topology {
    private val emptyEp: Option[Endpoint] = None
    private val idx =
      new AtomicReference[(HashRing[ReplicaSetId], m.MultiMap[ReplicaSetId, Endpoint])](
        (HashRing[ReplicaSetId](197), new m.HashMap[ReplicaSetId, m.Set[Endpoint]]
          with m.MultiMap[ReplicaSetId, Endpoint])
      )

    def select(key: String) = {
      val (r, i) = idx.get()
      i.get(r(key)).fold(emptyEp)(balance(_))
    }

    def read(in: InputStream) = update(reader(in))

    def write(out: OutputStream) =
      writer(idx.get._2.map(_._2.toSet).toList, out)

    private [kroto] def message(msg: Msg) = msg match {
      case Sync =>
        println("--> Sync received!")
      case Status =>
      case _ =>
    }

    private [kroto] def add(ep: Endpoint) = synchronized {
      val (r, i) = idx.get()
      if (!r.nodes.contains(ep.id)) r += ep.id
      i.get(ep.id).fold(i.put(ep.id, m.Set(ep)))(s => i.put(ep.id, s + ep))
    }

    private [kroto] def remove(ep: Endpoint) = synchronized {
      val i = idx.get()._2
      i.get(ep.id).foreach(s => i.put(ep.id, s.filterNot(_.equals(ep))))
    }

    private def update(epl: List[Set[Endpoint]]) = synchronized {
      val r = HashRing[ReplicaSetId](197)
      val i = new m.HashMap[ReplicaSetId, m.Set[Endpoint]]
        with m.MultiMap[ReplicaSetId, Endpoint]
      epl.foreach { set =>
        set.foreach { ep =>
          if (!r.nodes.contains(ep.id)) r += ep.id
          i.get(ep.id).fold(i.put(ep.id, m.Set(ep)))(s => i.put(ep.id, s + ep))
        }
      }
      idx.set((r, i))
    }
  }
}
