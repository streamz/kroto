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

import io.streamz.kroto.server.Command.Handler
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.Terminal

object Session {
  val banner =
    "    _...._\n" +
    "  .'   \\ _'.\n" +
    " /##\\__/##\\_\\      __             __     \n" +
    "|\\##/  \\##/  |    / /__ _______  / /____ \n" +
    "|/  \\__/  \\ _|   /  '_// __/ _ \\/ __/ _ \\\n" +
    " \\ _/##\\__/#/   /_/\\_\\/_/  \\___/\\__/\\___/\n" +
    "  '.\\##/__.'          selector topology test app\n" +
    "    `\"\"\"\"`"

  val help = Array(
    "kroto shell",
    "Usage: kroto> [help] [leader] [top] [map <k=v>] [select <key>]\n" +
    "  select <key>  show the selected endpoint for key",
    "  map <k=v k=v> sets the key mapping for the the topology",
    "  smap          show the topology mapper",
    "  top           show the kroto routing topology",
    "  leader        show the cluster leader (denoted by *)",
    "  help          show help").mkString("\n")
}
//noinspection ConvertExpressionToSAM
class Session private [server] (
  cmdHandler: Handler, terminal: Terminal)
  extends AutoCloseable {
  private val isRunning = new AtomicBoolean(true)
  private val rdr = LineReaderBuilder.builder()
    .terminal(terminal)
    .appName("kroto")
    .parser(new DefaultParser)
    .build()

  def start() = {
    println(Session.banner)
    println(Session.help)
    while (isRunning.get()) {
      try {
        val line = rdr.readLine("kroto> ")
        val cmd = line.split(" ").map(_.trim)
        val len = cmd.length
        val command = cmd.headOption.fold(Option.empty[ShellCommand]) {
          case "top" => Some(ShellCommand(TopologyCommand, List.empty))
          case "leader" => Some(ShellCommand(LeaderCommand, List.empty))
          case "select" =>
            if (len == 2) Some(ShellCommand(SelectorCommand, cmd.tail.toList))
            else Option.empty[ShellCommand]
          case "smap" => Some(ShellCommand(ShowMapCommand, List.empty))
          case "map" =>
            if (len > 1) Some(ShellCommand(MapCommand, cmd.tail.toList))
            else Option.empty[ShellCommand]
          case "help" =>
            println(Session.help)
            Option.empty[ShellCommand]
          case "quit" =>
            println("bye!")
            close()
            Option.empty[ShellCommand]
          case _ =>
            println(s"Unknown command: ${cmd(0)}")
            println(Session.help)
            Option.empty[ShellCommand]
        }
        command.foreach(cmdHandler(_, this))
      } catch {
        case _: Throwable =>
      }
    }
  }

  def println(s: String): Unit = terminal.writer().println(s)

  def close() = {
    isRunning.set(false)
    terminal.close()
  }
}
