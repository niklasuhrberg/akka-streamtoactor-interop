package com.triadicsystems.examples.passthrough


import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{PropsAdapter, _}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.WatchedActorTerminatedException
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.triadicsystems.examples.protocol.FlowMessages.Response
import com.triadicsystems.examples.passthrough.IdFlowMessages.{FlowMessageWithId, InvocationWithId}
import com.triadicsystems.examples.protocol.StreamToActorMessaging.StreamToActorMessage
import com.typesafe.scalalogging.LazyLogging

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait WithId {
  def id:UUID
}
/*
 This is the exact same stream as the StreamWithStageActor but with the use of PassthroughActorRefBackpressureProcessFlowStage
 to enable a pass-through like functionality. This fits e.g. real world Kafka processing where the Kafka commit related
 information must be available down-stream.

 See the documentation for StreamWithStageActor for the descriptions of the different scenarios.

 The target actor responds for the first Invocation messages too, letting you study the behaviour of the pass-through
 functionality for the cases one response and multiple responses respectively.
 With nothing changed the stream will process three elements causing four elements be emitted down-stream, the last of
 which has no pass-through element.

 One response logged:
 Got response (ResponseWithId(2146a07c-be8b-40f3-ab36-423bb773a8a7,Name1,5),Some(Passthrough-1))

 Two responses logged:
 Got response (ResponseWithId(ffcd9d79-788b-484c-9f0e-8d09e8e21d6b,Name3,90),Some(Passthrough-3))
 Got response (ResponseWithId(ffcd9d79-788b-484c-9f0e-8d09e8e21d6b,Name3,49),None)

 I.e. only the first response is accompanied with the pass-though element.

 */
object PassthroughStreamWithStageActor extends LazyLogging {
  implicit val timeout = Timeout(5.seconds)
  type PassThrough = String

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
    implicit val system: ActorSystem[_] = ctx.system
    import system.executionContext

    val targetActor = ctx.actorOf(PropsAdapter[StreamToActorMessage[FlowMessageWithId]](PassthroughTargetActor()))
    val actorFlow: Flow[(FlowMessageWithId, PassThrough), (FlowMessageWithId, Option[PassThrough]), NotUsed] =
      Flow.fromGraph(new PassthroughActorRefBackpressureProcessFlowStage[FlowMessageWithId, PassThrough](targetActor))
    val value = Source(1 to 3).map {
      case 4 =>
        logger.debug(s"Step 1, 2 arrived, will throw exception")
        throw new IllegalStateException("4 is a bad number from stream")
      case other =>
        logger.debug(s"Step 1 before the actorFlow with value $other")
        (InvocationWithId(UUID.randomUUID(), s"Name$other"), s"Passthrough-$other")
    }
      .via(actorFlow).recover { case e: WatchedActorTerminatedException =>
      println(s"Recovering from $e")
      Response("The end", 100)
    }
      .log("logflow", response => s"Got response $response")
      .runWith(Sink.ignore)


    value onComplete {
      case Success(value) => logger.debug("Stream completed successfully")
      case Failure(exception) => logger.error(s"Stream failed with $exception")
    }

    Behaviors.empty
  }

}
