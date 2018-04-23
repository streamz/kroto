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

trait RouterObject[A, B] extends AutoCloseable {
  def apply(a: A): B
}

object RouterObject {
  def apply[A, B](t: Topology): RouterObject[A, B] = {
    new RouterObject[A, B] {
      def apply(a: A): B = ???

      def close(): Unit = ???
    }
  }
}