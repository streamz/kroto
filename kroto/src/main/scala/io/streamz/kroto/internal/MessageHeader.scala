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

import java.io.{DataInput, DataOutput}
import java.util.function.Supplier

import io.streamz.kroto._
import org.jgroups.{Global, Header}
import org.jgroups.util.Streamable

object MessageHeader {
  val magicId: Short = 4270
}

class MessageHeader extends Header with Streamable {
  private var id: Int = -1

  def this(t: Msg) = {
    this()
    id = t.id
  }

  override
  val getMagicId: Short = MessageHeader.magicId

  override
  def writeTo(out: DataOutput) = out.writeInt(id)

  override
  def readFrom(in: DataInput) = id = in.readInt()

  override
  def create() = new Supplier[MessageHeader] {
    def get = new MessageHeader
  }

  override
  def serializedSize() = Global.INT_SIZE

  def toMsg: Option[Msg] = id match {
    case Sync.id => Sync.toOption
    case Status.id => Status.toOption
    case Hello.id => Hello.toOption
    case _ => None
  }
}
