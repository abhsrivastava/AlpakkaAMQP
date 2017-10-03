package com.abhi

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.alpakka.amqp.{AmqpConnectionUri, IncomingMessage, NamedQueueSourceSettings, QueueDeclaration}
import akka.stream.alpakka.amqp.javadsl.AmqpSource
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by ASrivastava on 10/2/17.
  */

object AlpakkaAMQPConsumer extends App {
   implicit val actorSystem = ActorSystem()
   implicit val actorMaterializer = ActorMaterializer()
   val queueName = "queue"
   val queueDeclaration = QueueDeclaration(queueName, durable = true)
   val uri = "amqp://abhi:abhi@abhisheks-mini:5672/myvhost"
   val amqpUri = AmqpConnectionUri(uri)
   val namedQueueSourceSettings = NamedQueueSourceSettings(amqpUri, queueName).withDeclarations(queueDeclaration)
   val source = AmqpSource.atMostOnceSource(namedQueueSourceSettings, bufferSize = 10)
   val flow1 = Flow[IncomingMessage].map(msg => msg.bytes)
   val flow2 = Flow[ByteString].map(_.utf8String)
   val sink = Sink.foreach[String](println)
   val graph = RunnableGraph.fromGraph(GraphDSL.create(sink){implicit builder =>
      s =>
      import GraphDSL.Implicits._
      source ~> flow1 ~> flow2 ~> s.in
      ClosedShape
   })
   val future = graph.run()
   future.onComplete{ _ =>
      actorSystem.terminate()
   }
   Await.result(actorSystem.whenTerminated, Duration.Inf)
}
