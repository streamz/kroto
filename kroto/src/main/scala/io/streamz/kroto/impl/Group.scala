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
package io.streamz.kroto.impl

import java.io.{InputStream, OutputStream}
import java.net.URI
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.typesafe.scalalogging.StrictLogging
import io.streamz.kroto.{GroupId, Topology}
import org.jgroups._
import org.jgroups.util.MessageBatch

trait Group {
  def isLeader: Boolean
  def join(): Unit
  def leave(): Unit
  def topology(): Topology
}

object Group {
  private val eg: Option[Group] = None
  def apply(
    uri: URI,
    id: GroupId,
    top: Topology): Option[Group] = ProtocolInfo(uri, id).fold(eg) { p =>
    import org.jgroups.conf.ClassConfigurator
    ClassConfigurator.add(MessageHeader.magicId, classOf[MessageHeader])
    Some {
      new Group with Receiver with StrictLogging {
        private val jc = new JChannel(p.get: _*)
        private val leader = new AtomicBoolean(false)
        private val worker = new SPSCQueue[View](update, 10)
        private val members = new AtomicReference[Set[Address]](Set())

        def isLeader = leader.get()

        def topology() = top

        def join(): Unit = {
          jc.setReceiver(this)
          jc.connect(id.value)
          ()
        }

        def leave() = {
          jc.disconnect()
          jc.close()
          ()
        }

        def receive(msg: Message) = Option[Header](
          msg.getHeader(MessageHeader.magicId)).foreach {
          case m: MessageHeader => m.toMsg.foreach(top.message)
          case _ => logger.info(s"---> discarding message:\n$msg")
        }

        def viewAccepted(view: View) = {
          val address = jc.address()
          import scala.collection.JavaConversions._
          leader.set(view.getMembers.headOption.fold(false) { a =>
            val lead = a.equals(address)
            println(s"$a is the leader; ${if (lead) "that is me!"}")
            lead
          })
          worker.push(view)
        }

        def suspect(suspect: Address) =
          println(s"---> $suspect is suspect")

        def receive(batch: MessageBatch) = {
          import scala.collection.JavaConversions._
          batch.foreach(receive)
        }

        def getState(out: OutputStream) = top.write(out)

        def setState(in: InputStream) = top.read(in)

        private def update(view: View) = {
          println(s"---> viewAccepted:\n$view")
          val vw = view match {
            case v: MergeView => mergeViews(v)
            case _ => view
          }

          import scala.collection.JavaConversions._
          val group = vw.getMembers.toSet
          println(s"---> $group is the new group")
          val leftGroup = members.get.diff(group)
          println(s"---> $leftGroup left the group")
          val joinedGroup = group.diff(members.getAndSet(group))
          println(s"---> $joinedGroup joined the group")
        }

        private def mergeViews(v: MergeView) = {
          // TODO: create a new thread to do the merge
          println(s"---> mergeViews:\n$v")
          v
        }
      }
    }
  }
}