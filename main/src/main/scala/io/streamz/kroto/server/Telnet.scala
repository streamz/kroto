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
package io.streamz.kroto.server

import java.util.concurrent.atomic.AtomicBoolean

import io.streamz.kroto._
import io.streamz.kroto.internal.Group
import org.jline.builtins.telnet._

import scala.collection.mutable
import scala.util.hashing.MurmurHash3

object Telnet {
  def apply(args: Array[String]): Option[AutoCloseable] =
    CmdLineParser.parse(args).fold(Option.empty[AutoCloseable]) { c =>
      val group = Group(
        c.uri,
        c.gid,
        Topology(
          Mapper.map(ReplicaSets(Map.empty),
            (s: String) => MurmurHash3.stringHash(s)),
          LoadBalancer.random))

      def newSelector: Option[Selector[String]] = {
        val replicas: Map[Int, ReplicaSetId] =
          c.rids.zipWithIndex.map(_.swap).toMap
        group.fold(Option.empty[Selector[String]]) { f =>
          Some(Selector(Endpoint(c.ep, replicas(0)), f))
        }
      }

      newSelector.fold(Option.empty[AutoCloseable]) { selector =>
        val ac = Some(new AutoCloseable {
          private val sessions = new mutable.HashSet[Session]
          private val shuttingDown = new AtomicBoolean(false)
          private val portListener = new PortListener("kroto", c.port, 10)
          portListener.setConnectionManager(
            new SessionManager((cmd: ShellCommand, s: Session) => {
              cmd.cmd match {
                case SelectorCommand =>
                  val ep = cmd.args.headOption
                    .fold(Option.empty[Endpoint])(selector.select)
                  ep.fold(s.println("no endpoint found")) { f =>
                    s.println(s"endpoint: ${f.ep.toASCIIString}")
                    s.println(s"replica:  ${f.id.value}")
                  }
                case TopologyCommand =>
                  s.println("topology:")
                  s.println(group.get.getTopology.toString)
                case LeaderCommand =>
                  val g = group.get
                  s.println(
                    s"cluster leader: ${
                      g.getLeader.fold("Unknown")(f =>
                        s"${f.toString}${if(g.isLeader) "*" else ""}")}")
                case ShowMapCommand =>
                  s.println("map:")
                  s.println(
                    group.get.getTopology.mapperToString)
                case MapCommand =>
                  val m = Command.parseMap(cmd.args)
                  s.println("new mapping:")
                  s.println(m.mkString("\n"))
                  group.get
                    .getTopology
                    .merge(
                      ReplicaSets(
                        m.map(
                          kv => (MurmurHash3.stringHash(kv._1).toLong, kv._2))))
                  group.get.merge()
                case _ =>
              }
            }))

          def close() = {
            shuttingDown.set(true)
            portListener.stop()
            sessions.foreach(close)
          }

          private def close(session: Session) = {
            session.close()
            sessions.remove(session)
          }
          selector.start()
          portListener.start()
        })
        ac
      }
    }
}

