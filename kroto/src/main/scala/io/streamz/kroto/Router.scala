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

import io.streamz.kroto.impl.Group

trait Router[A] extends AutoCloseable {
  def isLeader: Boolean
  def start(): Unit
  def route(key: A): Option[Endpoint]
}

object Router {
  def apply[A](
    serviceEndpoint: Endpoint,
    group: Group[A]): Router[A] = new Router[A] {

    def start(): Unit = group.join(serviceEndpoint)
    def route(key: A): Option[Endpoint] = group.topology().select(key)
    def isLeader: Boolean = group.isLeader
    def close(): Unit = group.leave()

    override
    def toString = group.topology().toString
  }
}
