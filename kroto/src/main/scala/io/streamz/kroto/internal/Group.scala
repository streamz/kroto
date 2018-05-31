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
package io.streamz.kroto.internal

import java.io.{InputStream, OutputStream}
import java.net.URI
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.typesafe.scalalogging.StrictLogging
import io.streamz.kroto._
import org.jgroups._

trait Group[A] {
  def isLeader: Boolean
  def getLeader: Option[LogicalAddress]
  def join(ep: Endpoint): Unit
  def leave(): Unit
  def topology(): Topology[A]
}

object Group {
  def apply[A](
    uri: URI,
    id: GroupId,
    top: Topology[A]): Option[Group[A]] =
    ProtocolInfo(uri, id).fold(Option.empty[Group[A]]) { p =>
    try {
      import org.jgroups.conf.ClassConfigurator
      ClassConfigurator.add(MessageHeader.magicId, classOf[MessageHeader])
    } catch {
      case _: Throwable => // eat it
    }
    Some {
      new Group[A] with Receiver with StrictLogging {
        private val jc = new JChannel(p.get: _*)
        private val clusterLeader = new AtomicBoolean(false)
        private val leader = new AtomicReference[Option[LogicalAddress]](None)
        private val worker = new SPSCQueue[View](update, 10)
        private val members = new AtomicReference[Set[Address]](Set())

        def isLeader = clusterLeader.get()

        def getLeader: Option[LogicalAddress] = leader.get()

        def topology() = top

        def join(ep: Endpoint): Unit = {
          jc.setReceiver(this)
          jc.setDiscardOwnMessages(true)
          jc.connect(id.value)

          val sep = ep.copy(la = Some(LogicalAddress(jc.getAddressAsString)))
          topology().add(sep)

          if (!isLeader) sendHelloMsg(sep)
        }

        def leave() = {
          jc.disconnect()
          jc.close()
          ()
        }

        def receive(msg: Message) = Option[Header](
          msg.getHeader(MessageHeader.magicId)).foreach {
          case m: MessageHeader =>
            m.toMsg.foreach {
              case Sync =>
                logger.debug("topology has changed, getting state")
                jc.getState(null, 5000)
              case Hello =>
                logger.debug(s"`Hello` received: $msg")
                top.add(Endpoint.fromBuffer(msg.getBuffer))
                sendSyncMsg()
              case _ =>
            }
          case _ => logger.debug(s"discarding message:\n$msg")
        }

        def viewAccepted(view: View) = {
          val address = jc.address()
          import scala.collection.JavaConversions._
          clusterLeader.set(view.getMembers.headOption.fold {
            leader.set(None)
            false
          } { a =>
            leader.set(Some(LogicalAddress(a.toString)))
            a.equals(address)
          })
          worker.push(view)
        }

        // TODO: mark node down
        def suspect(suspect: Address) =
          logger.error(s"... $suspect is suspect")

        def getState(out: OutputStream) = top.write(out)

        def setState(in: InputStream) = top.read(in)

        private def update(view: View) = {
          val vw = view match {
            case v: MergeView => mergeViews(v)
            case _ => view
          }

          import scala.collection.JavaConversions._
          val group = vw.getMembers.toSet
          val leftGroup = members.get.diff(group)
          leftGroup.foreach(a => top.remove(LogicalAddress(a.toString)))
          members.getAndSet(group)
        }

        private def mergeViews(v: MergeView) = {
          import scala.collection.JavaConversions._
          val partition = v.getSubgroups.headOption
          val address = jc.getAddress
          val sync = partition.fold(false) { p =>
            !p.getMembers.contains(address)
          }
          if (sync) {
            if (!isLeader)
              top.find(LogicalAddress(address.toString)).foreach(sendHelloMsg)
            else jc.getState(null, 5000)
          }
          v
        }

        private def sendHelloMsg(dest: Endpoint) = {
          val msg = new Message(true)
          val hdr = new MessageHeader(Hello)
          msg.putHeader(hdr.getMagicId, hdr)
          // send back to the coordinator
          msg.setDest(jc.getView.getMembers.get(0))
          msg.setBuffer(Endpoint.toBuffer(dest))
          jc.send(msg)
        }

        private def sendSyncMsg() = {
          // sync message will force followers to get state
          val msg = new Message(true)
          val hdr = new MessageHeader(Sync)
          msg.putHeader(hdr.getMagicId, hdr)
          logger.debug(s"sending sync message: $msg")
          jc.send(msg)
        }
      }
    }
  }
}