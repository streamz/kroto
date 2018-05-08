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

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, URI}
import java.util.concurrent.atomic.AtomicBoolean

import io.streamz.kroto.impl.Group
import scopt.OptionParser

import scala.language.postfixOps

case class Request(key: String)
case class Config(
  gid: String = "",
  rid: String = "",
  uri: URI = null,
  ep: URI = null,
  port: Int = 9999,
  sets: Seq[String] = Seq(),
  hpl: Seq[String] = Seq())

class TestServer(port: Int, router: Router[String]) {
  val running = new AtomicBoolean(false)
  val thread = new Thread {
    override def run() = {
      println(s"TestServer running on port: $port")
      val serverSocket = new ServerSocket(port)
      while (running.get) {
        val sock = serverSocket.accept
        val os = sock.getOutputStream
        val br = new BufferedReader(new InputStreamReader(sock.getInputStream))
        val pw = new PrintWriter(os, true)
        while (running.get && !sock.isClosed) {
          pw.print("%>")
          pw.flush()
          val str = br.readLine
          if (str.compareToIgnoreCase("quit") == 0) sock.close()
          else {
            pw.println(s"routed $str to: ${
              router.route(str).fold("No Route")(_.toString)}")
          }
          pw.flush()
        }
        pw.println("Bye!")
        pw.close()
        br.close()
        if (sock.isConnected) sock.close()
      }
      serverSocket.close()
    }
  }

  def start() = {
    running.set(true)
    router.start()
    thread.start()
  }

  def stop() = {
    running.set(false)
    println("dumping topology:")
    println(router.toString)
    router.close()
  }
}

object Main extends App {
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = {
      server.foreach(_.stop())
    }
  })
  val parser = new OptionParser[Config]("kroto-main-node") {
    head("kroto-main-node", "x.x")
    opt[String]('g', "group") valueName "<group>" action {
      (x,c) => c.copy(gid = x) } text "groupId to join" required()
    opt[String]('r', "replica") valueName "<replica>" action {
      (x,c) => c.copy(rid = x) } text "replica set id" required()
    opt[URI]('u', "uri") valueName "<uri>" action {
      (x,c) => c.copy(uri = x) } text "uri for this node" required()
    opt[URI]('e', "endpoint") valueName "<endpoint>" action {
      (x,c) => c.copy(ep = x) } text "endpoint to route to" required()
    opt[Int]('p', "port") valueName "<port>" action {
      (x,c) => c.copy(port = x) } text "telnet port" required()
    opt[Seq[String]]('s', "replicas") valueName
      "<r1>,<r2>..." action {
      (x,c) => c.copy(sets = x) } text "list of replica set ids" required()
    opt[Seq[String]]('h', "host:port") valueName
      "<host:port1>,<host:port2>..." action {
      (x,c) => c.copy(hpl = x) } text "list of nodes host:port" optional()
  }
  val server: Option[TestServer] = parser.parse(args, Config()) match {
    case Some(c) =>
      val uri = {
        if (c.hpl.isEmpty) c.uri
        else new URI(c.uri + "?" + c.hpl.map("node=" + _).mkString("&"))
      }
      val replicas: Map[Int, ReplicaSetId] =
        c.sets.map(ReplicaSetId).zipWithIndex.map(_.swap).toMap

      val g = Group(
        uri,
        GroupId(c.gid),
        Topology(
          (s: String) => {
            if (replicas.isEmpty) None
            else replicas.get(s.hashCode % replicas.size)
          },
          LoadBalancer.random))
      val ro = g.fold(Option.empty[Router[String]]) { f =>
        Some(Router(Endpoint(c.ep, ReplicaSetId(c.rid)), f))
      }
      ro.fold(Option.empty[TestServer])(r => Some(new TestServer(c.port, r)))
    case _ => None
  }
  server.foreach(_.start())
}
