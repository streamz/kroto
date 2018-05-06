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

import java.io.{DataInputStream, DataOutputStream, InputStream, OutputStream}
import java.net.URI

import io.streamz.kroto.impl.Group
import scopt.OptionParser

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class Request(key: String)
case class Config(
  gid: String = "",
  rid: String = "",
  uri: URI = null,
  ep: URI = null,
  hpl: Seq[String] = Seq())

object Main extends App {
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = {
      println("dumping topology:")
      println(router.get.toString)
      router.foreach(_.close())
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
    opt[Seq[String]]('h', "host:port") valueName
      "<host:port1>,<host:port2>..." action {
      (x,c) => c.copy(hpl = x) } text "list of nodes host:port" optional()
  }

  val router: Option[Router] = parser.parse(args, Config()) match {
    case Some(c) =>
      val uri = {
        if (c.hpl.isEmpty) c.uri
        else new URI(c.uri + "?" + c.hpl.map("node=" + _).mkString("&"))
      }
      val g = Group(
        uri,
        GroupId(c.gid),
        Topology(
          (s: mutable.Set[Endpoint]) => s.headOption,
          (in: InputStream) => SimpleSerDe.read(in),
          (epl: List[Set[Endpoint]], out: OutputStream) =>
            SimpleSerDe.write(epl, out)))
      g.fold(None.asInstanceOf[Option[Router]]) { f =>
        Some(Router(Endpoint(c.ep, ReplicaSetId(c.rid)), f))
      }
    case _ => None
  }
  router.foreach(_.start())
}

object SimpleSerDe {
  def read(in: InputStream): List[Set[Endpoint]] = {
    val is = new DataInputStream(in)
    val listLen = is.readInt()
    val listBuffer = new ListBuffer[Set[Endpoint]]
    0 until listLen foreach { _ =>
      val setSize = is.readInt()
      val s = new mutable.HashSet[Endpoint]()
      0 until setSize foreach { _ =>
        val uri = new URI(is.readUTF())
        val rep = ReplicaSetId(is.readUTF())
        val la = is.readUTF()
        s.add(Endpoint(uri, rep, if (la.nonEmpty) Some(LogicalAddress(la)) else None))
      }
      listBuffer += s.toSet
    }
    val l = listBuffer.toList
    println(s"=== read $l")
    l
  }

  def write(epl: List[Set[Endpoint]], out: OutputStream) = {
    println(s"=== write: $epl")
    val os = new DataOutputStream(out)
    os.writeInt(epl.length)
    epl.foreach { set =>
      os.writeInt(set.size)
      set.foreach { ep =>
        os.writeUTF(ep.ep.toString)
        os.writeUTF(ep.id.value)
        os.writeUTF(ep.la.fold("")(_.value))
      }
    }
    os.flush()
  }
}