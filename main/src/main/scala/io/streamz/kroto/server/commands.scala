/*
--------------------------------------------------------------------------------
    Copyright 2018 streamz.io
    Cluster Hash Ring Router based on JGroups

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
package io.streamz.kroto.server

import io.streamz.kroto.ReplicaSetId

sealed trait Command
case object TopologyCommand extends Command
case object SelectorCommand extends Command
case object MapCommand extends Command
case class ShellCommand(cmd: Command, args: List[String])

object Command {
  type Handler = (ShellCommand, Session) => Unit

  def parseMap(args: List[String]): Map[String, ReplicaSetId] = {
    args.flatMap { f =>
      val arr = f.trim.split("=")
      if (arr.length == 2) Some(arr(0).trim -> ReplicaSetId(arr(1).trim))
      else None
    }.toMap
  }
}
