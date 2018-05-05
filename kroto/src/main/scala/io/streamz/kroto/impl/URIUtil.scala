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

import java.net.URI

import scala.collection.mutable

object URIUtil {
  def parseQuery(uri: URI): Map[String, List[String]] = {
    val m = new mutable.HashMap[String, List[String]]
    val query = Option(uri.getQuery)

    query.foreach(_.split("&").foreach { f =>
      val kv = f.split("=")
      if (kv.length > 1)
        m.put(kv(0), m.get(kv(0)).fold(List(kv(1)))(_ ++ List(kv(1))))
    })

    m.toMap
  }
}
