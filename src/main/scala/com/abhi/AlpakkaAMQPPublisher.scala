package com.abhi

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import akka.stream.scaladsl._

/**
  * Created by ASrivastava on 10/2/17.
  */
object AlpakkaAMQPPublisher extends App {
   implicit val actorSystem = ActorSystem()
   implicit val actorMaterializer = ActorMaterializer()
   val queueName = "myqueue"
   val queueDeclaration = QueueDeclaration(queueName, durable = true)
   val uri = "amqp://abhi:abhi@abhisheks-mini:5672/myvhost"
   val settings = AmqpSinkSettings(AmqpConnectionUri(uri))
      .withRoutingKey("foobar")
      .withExchange("exchange")
      .withDeclarations(queueDeclaration)
   val amqpSink = AmqpSink.simple(settings)

   val resource = getClass.getResource("/countrycapital.csv")
   val path = Paths.get(resource.toURI)
   val source = FileIO.fromPath(path)

   val graph = RunnableGraph.fromGraph(GraphDSL.create(amqpSink){implicit builder =>
      s =>
         import GraphDSL.Implicits._
         source ~> s.in
         ClosedShape
   })

   val future = graph.run()

   future.onComplete { _ =>
      actorSystem.terminate()
   }
   Await.result(actorSystem.whenTerminated, Duration.Inf)
}
