package com.triadicsystems.examples.askbased

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.WatchedActorTerminatedException
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import com.triadicsystems.examples.askbased.TargetActor.{Invocation, ProcessMessage, Response, StreamCompleted, StreamFailed}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

/*
 This stream showcases the ask based interoperability with the following scenarios.
 1. Happy flow: Processes 5 elements each sending an Invocation to the target actor and receiving a Response.
    Some logging is done enabling you to study the behaviour.
    Note that when the stream completes a StreamCompleted message is sent to the target actor.
    This is to show that the lifecycle can indeed be handled in the ask based flow too.
 2. Failure in the stream. Change the "case 6" to e.g. "case 2" and watch the target actor get the StreamFailed message
    ending the lifecycle.
 3. Failure with the target actor throwing IllegalStateException. Change the var counter (in TargetActor) to initial
    value 1 and study the stream recovering from akka.stream.WatchedActorTerminatedException. The recovery logic causes the exception being
    handled by emitting Response("The end", 100). Before this final element, only one response makes it to the Sink and
    this is logged with e.g. "[Got response Response(Invocation no 1,17)]"
 4. Failure caused by an ask timeout. The behaviour is triggered by setting the var counter to the value 6 making it
    comparable to 3 in that also then only one response makes it the the Sink. However, since the target actor only
    is slower (causing the timeout) it will get three Invocation messages, the last of which happens after the stream
    fails since it is already in the mailbox of the actor. Only after the third Invocation message does the StreamFailed
    arrive. Although this may seem odd at a first glance, it makes full sense.
 */
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
        throw new IllegalStateException("3 is a bad number from stream")
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
