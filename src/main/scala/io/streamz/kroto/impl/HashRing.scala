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

import scala.collection.immutable.Seq
import scala.collection.{mutable => m}
import scala.util.hashing.MurmurHash3

object HashRing {
  def apply[A](replicas: Int) = new HashRing[A](replicas)
  def apply[A](points: Seq[A], replicas: Int) = new HashRing[A](points, replicas)
}

class HashRing[A] private [impl] (replicas: Int) {
  private val delimiter = "-"
  private val buffer = m.Buffer[A]()
  private var keys = m.TreeSet[Int]()
  private var ring = m.Map[Int, A]()

  def this(points: Seq[A], replicas: Int) = {
    this(replicas: Int)
    points.foreach(this += _)
  }

  def nodes: List[A] = buffer.toList
  def +=(node: A): Unit = synchronized {
    if (!buffer.contains(node)) {
      buffer += node
      1 to replicas foreach {
        replica =>
          val key = hashFor((node + delimiter + replica).getBytes("UTF-8"))
          ring += (key -> node)
          keys += key
      }
    }
  }

  def -=(node: A): Unit = synchronized {
    buffer -= node
    1 to replicas foreach {
      replica =>
        val key = hashFor((node + delimiter + replica).getBytes("UTF-8"))
        ring -= key
        keys -= key
    }
  }

  def apply(key: String): A = {
    val bKey = key.getBytes("UTF-8")
    val hash = hashFor(bKey)
    if (keys.contains(hash)) ring(hash)
    else {
      if (hash < keys.firstKey) ring(keys.firstKey)
      else if (hash > keys.lastKey) ring(keys.lastKey)
      else ring(keys.rangeImpl(None, Some(hash)).lastKey)
    }
  }

  private def hashFor(bytes: Array[Byte]) =
    math.abs(MurmurHash3.arrayHash(bytes))
}