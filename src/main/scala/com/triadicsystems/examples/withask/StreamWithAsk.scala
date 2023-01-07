package com.triadicsystems.examples.withask

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.WatchedActorTerminatedException
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.triadicsystems.examples.withask.TargetActor.{Invocation, ProcessMessage, Response, StreamCompleted, StreamFailed}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object StreamWithAsk extends LazyLogging {


  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
    implicit val timeout = Timeout(5.seconds)
    implicit val system: ActorSystem[_] = ctx.system
    import system.executionContext

    val targetActor: ActorRef[TargetActor.ProcessMessage] = ctx.spawn(TargetActor(), "targetActor")
    val askFlow: Flow[Int, ProcessMessage, NotUsed] =
      ActorFlow.ask[Int, Invocation, ProcessMessage](targetActor)((in, q) => Invocation(s"Invocation no $in", q))
    val value = Source(1 to 5).map {
      case 6 => // Change to a number 1-5 to study the behavior when the actor fails
        logger.debug(s"Step 1, 3 arrived, will throw exception")
        throw new IllegalStateException("3 is a bad number")
      case other =>
        logger.debug(s"Step 1 before the ask with value $other")
        other
    }
      .via(askFlow).recover { case e: WatchedActorTerminatedException =>
      println(s"Recovering from $e")
      Response("The end", 100)
    }
      .runWith(Sink.foreach(response => logger.debug(s"Got response $response")))
    // This handles the propagation of stream failure to the actor
    value onComplete {
      case Success(value) => logger.debug("Stream with ask completed successfully")
      targetActor ! StreamCompleted
      case Failure(exception) => logger.error(s"Stream with ask failed with ${exception.getMessage}")
        targetActor ! StreamFailed
    }
    Behaviors.empty
  }


}
