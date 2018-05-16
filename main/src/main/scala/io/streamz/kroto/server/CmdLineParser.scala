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

import java.net.URI

import io.streamz.kroto.{GroupId, ReplicaSetId}
import org.jline.builtins.Options

object CmdLineParser {
  case class Config(
    gid: GroupId,
    uri: URI,
    ep: URI,
    rids: Set[ReplicaSetId],
    port: Int)

  def parse(args: Array[String]): Option[Config] = {
    val usage = Array(
      "krotod - start simple kroto daemon",
      "Usage: krotod [-h host] [-p port] [-u uri] [-e endpoint] " +
        "[-g group] [-r replicas]",
      "  -p --port=Port         listen port",
      "  -u --uri=URI           cluster configuration uri",
      "  -e --endpoint=Endpoint service endpoint",
      "  -g --group=Group       cluster or group identifier",
      "  -r --replicas=Replicas replica set identifiers , separated",
      "  -? --help              show help")

    val opt = Options.compile(usage).parse(args.map(_.asInstanceOf[Object]))

    if (
      opt.isSet("help") ||
      !opt.isSet("port") ||
      !opt.isSet("uri") ||
      !opt.isSet("group") ||
      !opt.isSet("replicas")) {
      opt.usage(System.err)
      None
    }
    else {
      val str = opt.get("replicas")
      val reps = str.split(",").map(f => ReplicaSetId(f.trim)).toSet
      Some(
        Config(
          GroupId(opt.get("group")),
          new URI(opt.get("uri")),
          new URI(opt.get("endpoint")),
          reps,
          opt.get("port").toInt
        ))
    }
  }
}
