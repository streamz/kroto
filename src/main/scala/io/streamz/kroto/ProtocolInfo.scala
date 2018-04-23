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
package io.streamz.kroto

import java.net.{InetAddress, InetSocketAddress, URI}

import io.streamz.kroto.impl.URIUtil
import org.jgroups.protocols._
import org.jgroups.protocols.pbcast.{GMS, NAKACK2, STABLE, STATE_TRANSFER}
import org.jgroups.stack.Protocol

import scala.collection.JavaConversions

object ProtocolInfo {
  def apply(uri: URI): Option[ProtocolInfo] = {
    val scheme = uri.getScheme
    scheme match {
      case "tcp" | "udp" =>
        Some(new ProtocolInfo {
          def apply(): Array[Protocol] =
            if (scheme.compareTo("tcp") == 0) tcpProto(uri) else udpProto(uri)
          val clusterId: String = uri.getPath
        })
      case _ => None
    }
  }

  private def tcpProto(uri: URI): Array[Protocol] = {
    val p = URIUtil.parseQuery(uri)
    val tcpNio = new TCP_NIO2
    tcpNio.setBindAddress(InetAddress.getByName(uri.getHost))
    tcpNio.setBindPort(uri.getPort)
    tcpNio.setThreadPoolMaxThreads(
      p.get("max_threads")
        .fold(Runtime.getRuntime.availableProcessors())(_(0).toInt))
    tcpNio.setThreadPoolKeepAliveTime(p.get("max_threads")
      .fold(30000)(_(0).toInt))
    tcpNio.setConnExpireTime(p.get("conn_timeout")
      .fold(30000)(_(0).toInt))

    /*
    tcpNio.setValue("recv_buf_size", p.get("recv_buf_size")
      .fold(130000)(_(0).toInt))
    tcpNio.setValue("send_buf_size", p.get("send_buf_size")
      .fold(130000)(_(0).toInt))*/

    val tcpPing = new TCPPING
    val hosts = p.get("nodes").fold(List[InetSocketAddress]()) { f =>
      f.flatMap { s =>
        val hp = s.split(":")
        if (hp.length > 1) Some(new InetSocketAddress(hp(0), hp(1).toInt))
        else None
      }
    }
    import JavaConversions._
    tcpPing.setInitialHosts(hosts)

    val merge3 = new MERGE3
    merge3.setMaxInterval(p.get("min_interval").fold(10000)(_(0).toInt))
    merge3.setMaxInterval(p.get("max_interval").fold(30000)(_(0).toInt))

    val fdAll2 = new FD_ALL2
    fdAll2.setInterval(p.get("hb_interval").fold(8000)(_(0).toInt))
    fdAll2.setTimeout(p.get("hb_timeout").fold(30000)(_(0).toInt))

    val nakack2 = new NAKACK2
    nakack2.setUseMcastXmit(false)
    nakack2.setDiscardDeliveredMsgs(p.get("discard_msgs")
      .fold(false)(_(0).toBoolean))
    nakack2.setLogDiscardMessages(p.get("dbg_ack")
      .fold(false)(_(0).toBoolean))

    val stable = new STABLE
    stable.setDesiredAverageGossip(
      p.get("st_gossip").fold(50000L)(_(0).toLong))
    stable.setMaxBytes(
      p.get("st_max_size_mb").fold(4194304L)(_(0).toLong))

    val rsvp = new RSVP()
    /*
    rsvp.setValue("timeout", p.get("rsvp_timeout").fold(60000)(_(0).toInt))
    rsvp.setValue("resend_interval", p.get("rsvp_resend").fold(500)(_(0).toInt))
    rsvp.setValue("ack_on_delivery", p.get("rsvp_ack")
      .fold(false)(_(0).toBoolean))
*/
    Array(
      tcpNio,
      tcpPing,
      merge3,
      new FD_SOCK,
      fdAll2,
      new VERIFY_SUSPECT,
      new BARRIER,
      nakack2,
      new UNICAST3,
      stable,
      new GMS,
      new MFC_NB,
      new FRAG3,
      rsvp,
      new STATE_TRANSFER)
  }

  private def udpProto(uri: URI): Array[Protocol] = {
    val p = URIUtil.parseQuery(uri)
    val udp = new UDP

    val merge3 = new MERGE3
    merge3.setMaxInterval(p.get("min_interval").fold(10000)(_(0).toInt))
    merge3.setMaxInterval(p.get("max_interval").fold(30000)(_(0).toInt))

    val nakack2 = new NAKACK2
    nakack2.setUseMcastXmit(false)
    nakack2.setDiscardDeliveredMsgs(p.get("discard_msgs")
      .fold(false)(_(0).toBoolean))
    nakack2.setLogDiscardMessages(p.get("dbg_ack")
      .fold(false)(_(0).toBoolean))

    val stable = new STABLE
    stable.setDesiredAverageGossip(
      p.get("st_gossip").fold(50000L)(_(0).toLong))
    stable.setMaxBytes(
      p.get("st_max_size_mb").fold(4194304L)(_(0).toLong))

    val rsvp = new RSVP()
    /*
    rsvp.setValue("timeout", p.get("rsvp_timeout").fold(60000)(_(0).toInt))
    rsvp.setValue("resend_interval", p.get("rsvp_resend").fold(500)(_(0).toInt))
    rsvp.setValue("ack_on_delivery", p.get("rsvp_ack")
      .fold(false)(_(0).toBoolean))
*/
    Array(
      udp,
      new PING,
      merge3,
      new FD_SOCK,
      new FD_ALL2,
      new VERIFY_SUSPECT,
      new BARRIER,
      nakack2,
      new UNICAST3,
      stable,
      new GMS,
      new MFC_NB,
      new FRAG3,
      rsvp,
      new STATE_TRANSFER)
  }
}

trait ProtocolInfo {
  def apply(): Array[Protocol]
  def clusterId: String
}