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

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import io.streamz.kroto._
import io.streamz.kroto.impl.Group
import org.jline.builtins.telnet._

import scala.collection.mutable

object Telnet {
  def apply(args: Array[String]): Option[AutoCloseable] =
    CmdLineParser.parse(args).fold(Option.empty[AutoCloseable]) { c =>
      val mapRef = new AtomicReference[Map[String, ReplicaSetId]]

      def newRouter: Option[Selector[String]] = {
        val replicas: Map[Int, ReplicaSetId] =
          c.rids.zipWithIndex.map(_.swap).toMap
        val g = Group(
          c.uri,
          c.gid,
          Topology(
            Mappers.mapped(mapRef),
            LoadBalancers.random))
        g.fold(Option.empty[Selector[String]]) { f =>
          Some(Selector(Endpoint(c.ep, replicas(0)), f))
        }
      }

      newRouter.fold(Option.empty[AutoCloseable]) { selector =>
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
                  s.println(selector.toString)
                case MapCommand =>
                  val m = Command.parseMap(cmd.args)
                  s.println("new mapping:")
                  s.println(m.mkString("\n"))
                  mapRef.set(m)
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

