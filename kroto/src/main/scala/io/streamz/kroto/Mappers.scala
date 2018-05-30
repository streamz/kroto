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

import java.util.concurrent.atomic.AtomicReference

import io.streamz.kroto.internal.HashRing

object Mappers {
  def mod[A](
    r: ReplicaSet[Int],
    f: A => Int): A => Option[ReplicaSetId] =
    (a: A) => {
      if (r.value.isEmpty) None
      else Some(r.value(f(a) % r.value.size))
    }

  def ring[A](
    r: ReplicaSet[Int],
    f: A => String): A => Option[ReplicaSetId] = {
    val ring = new HashRing[ReplicaSetId](r.value.values.toList, 197)
    a: A => ring(f(a))
  }

  def mapped[A](
    m: AtomicReference[ReplicaSet[A]]): A => Option[ReplicaSetId] =
    (a: A) => m.get.value.get(a)
}
