/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend.activemq.stream

import akka.Done
import akka.actor.ActorSystem
import akka.camel.{ CamelExtension, CamelMessage }
import akka.stream.scaladsl.{ Flow, Keep, Sink }
import com.github.dnvriend.activemq.extension.ActiveMqExtension

import scala.concurrent.{ ExecutionContext, Future }

object ActiveMqSink {
  import scala.collection.JavaConversions._
  def apply[T](config: String, qos: Int = 8)(implicit ec: ExecutionContext, system: ActorSystem, builder: MessageBuilder[T, CamelMessage]): Sink[T, Future[Done]] = {
    val template = CamelExtension(system).template
    Flow[T].mapAsync(qos) {
      case payload ⇒
        ActiveMqExtension(system).producerFor(config).map { uri ⇒
          val camelMessage = builder.build(payload)
          template.sendBodyAndHeaders(uri, camelMessage.body, camelMessage.headers.mapValues(_.asInstanceOf[AnyRef]))
        }
    }.toMat(Sink.ignore)(Keep.right)
  }
}
