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

import java.net.URI

sealed trait Msg {
  def id: Int
  def toOption: Option[Msg]
}
case object Sync extends Msg {
  val id = 0
  val toOption = Some(Sync)
}
case object Status extends Msg {
  val id = 1
  val toOption = Some(Status)
}
case object Update extends Msg {
  val id = 2
  val toOption = Some(Update)
}

/**
  * The Id of the group
  * @param value string id
  */
case class GroupId(value: String) extends AnyVal

/**
  * The Id of the replica set
  * @param value string id
  */
case class ReplicaSetId(value: String) extends AnyVal

/**
  * An endpoint URI
  * @param ep Endpoint
  * @param id ReplicaSetId
  */
case class Endpoint(ep: URI, id: ReplicaSetId)

