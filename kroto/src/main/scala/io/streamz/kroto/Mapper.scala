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
package io.streamz.kroto

import java.util.concurrent.atomic.AtomicReference

import io.streamz.kroto.internal.HashRing

trait Mapper[A] {
  def apply(a: A): Option[ReplicaSetId]
  def replicas: AtomicReference[ReplicaSets]
  def getReplicas: Option[ReplicaSets]
  def merge(rs: ReplicaSets) = {
    val reps = replicas.get().value
    val keys = rs.value.keySet ++ reps.keySet
    val map = keys.map(k => (k, rs.value.getOrElse(k, reps(k)))).toMap
    replicas.set(ReplicaSets(map))
  }
}

object Mapper {
  def mod[A](
    r: ReplicaSets,
    f: A => Long): Mapper[A] = new Mapper[A] {
    val replicas = new AtomicReference[ReplicaSets](r)
    def apply(a: A) = {
      val rs = replicas.get()
      val m = if (rs == null) Map.empty[Long, ReplicaSetId] else rs.value
      if (m.isEmpty) None
      else Some(m(f(a) % m.size))
    }
    def getReplicas = Option(replicas.get)
  }

  def ring[A](
    r: ReplicaSets,
    f: A => String): Mapper[A] = new Mapper[A] {
    val replicas = new AtomicReference[ReplicaSets](r)
    val ring = HashRing[ReplicaSetId](replicas.get.value.values.toList, 197)
    def apply(a: A) = ring(f(a))
    def getReplicas = Option(replicas.get)
  }

  def map[A](r: ReplicaSets, f: A => Long): Mapper[A] = new Mapper[A] {
    val replicas = new AtomicReference[ReplicaSets](r)
    def apply(a: A) = replicas.get.value.get(f(a))
    def getReplicas = Option(replicas.get)
  }
}
