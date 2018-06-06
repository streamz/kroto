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

import java.io.{InputStream, OutputStream, PrintStream}
import java.util.UUID

import io.streamz.kroto.server.Command.Handler
import org.jline.builtins.telnet._
import org.jline.terminal.{Size, TerminalBuilder}

class SessionManager(cmdHandler: Handler)
  extends ConnectionManager(
    1000, 5 * 60 * 1000, 5 * 60 * 1000, 60 * 1000, null, null, false) {
  import scala.collection.JavaConverters._
  private val sessions =
    new java.util.concurrent.ConcurrentHashMap[UUID, Session].asScala
  def createConnection(tg: ThreadGroup, cd: ConnectionData) =
    new Connection(tg, cd) {
      val uid = UUID.randomUUID()
      var io: TelnetIO = _
      def doRun() = {
        io = new TelnetIO
        io.setConnection(this)
        io.initIO()

        val in = new InputStream {
          def read() = io.read()
          override
          def read(b: Array[Byte], off: Int, len: Int) = {
            val r = read()
            if (r > 0) {
              b.update(off, r.toByte)
              1
            } else -1
          }
        }

        val out = new PrintStream(new OutputStream {
          override def write(b: Int) = io.write(b)
          override def flush() = io.flush()
        })

        val terminal = TerminalBuilder.builder
          .`type`(cd.getNegotiatedTerminalType.toLowerCase)
          .streams(in, out)
          .system(false)
          .name("telnet").build
        terminal.setSize(new Size(cd.getTerminalColumns, cd.getTerminalRows))

        addConnectionListener(new ConnectionListener {
          override
          def connectionTerminalGeometryChanged(ce: ConnectionEvent) = {
            import org.jline.terminal.Terminal.Signal
            terminal.setSize(new Size(cd.getTerminalColumns, cd.getTerminalRows))
            terminal.raise(Signal.WINCH)
          }
        })
        // attach a session
        val session = new Session(cmdHandler, terminal)
        sessions.put(uid, session)
        session.start()
      }

      def doClose() = {
        sessions.remove(uid).foreach(_.close())
        io.closeOutput()
        io.closeInput()
      }
  }

  override
  def stop() = {
    super.stop()
    sessions.foreach(_._2.close())
    sessions.clear()
  }
}
