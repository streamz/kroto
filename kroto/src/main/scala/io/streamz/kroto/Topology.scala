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

  /**
    * Read in and update the topology
    * @param in InputStream
    */
  def read(in: InputStream): Unit

  /**
    * Write the topology to the OutputStream
    * @param out OutputStream
    */
  def write(out: OutputStream): Unit

  /**
    * Find the endpoint given a LogicalAddress
    * @param la LogicalAddress
    * @return optional Endpoint
    */
  def find(la: LogicalAddress): Option[Endpoint]

  private [kroto] def add(ep: Endpoint)
  private [kroto] def remove(la: LogicalAddress)
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
    private val index =
      new AtomicReference[(HashRing[ReplicaSetId], m.MultiMap[ReplicaSetId, Endpoint])](
        (HashRing[ReplicaSetId](197), new m.HashMap[ReplicaSetId, m.Set[Endpoint]]
          with m.MultiMap[ReplicaSetId, Endpoint])
      )

    def select(key: String) = {
      val (r, i) = index.get()
      i.get(r(key)).fold(emptyEp)(balance(_))
    }

    def read(in: InputStream) = update(reader(in))

    def write(out: OutputStream) =
      writer(index.get._2.map(_._2.toSet).toList, out)

    def find(la: LogicalAddress) = {
      val i = index.get()._2
      i.values.flatten
        .find(ep => ep.la.fold(false)(a => a.value.equals(la.value)))
    }

    override
    def toString = {
      val sb = new StringBuilder
      index.get()._2.foreach { set =>
        sb.append("replicaSetId: ")
          .append(set._1.value)
        set._2.foreach { ep =>
          sb.append("\n\t")
            .append(s"address: ${ep.la.fold("NA")(_.value)}")
            .append("\n\t")
            .append(s"endpoint: ${ep.ep}")
        }
        sb.append("\n")
      }
      sb.toString()
    }

    private [kroto] def add(ep: Endpoint) = synchronized {
      val (r, i) = index.get()
      if (!r.nodes.contains(ep.id)) r += ep.id
      i.get(ep.id).fold(i.put(ep.id, m.Set(ep)))(s => i.put(ep.id, s + ep))
    }

    private [kroto] def remove(la: LogicalAddress) = synchronized {
      val i = index.get()._2
      i.foreach { kv =>
        i.put(
          kv._1,
          kv._2.filterNot(_.la.fold(false)(a => a.value.equals(la.value))))
      }
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
      index.set((r, i))
    }
  }
}