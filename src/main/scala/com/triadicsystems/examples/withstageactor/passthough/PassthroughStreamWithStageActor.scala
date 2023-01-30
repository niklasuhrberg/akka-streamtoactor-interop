package com.triadicsystems.examples.withstageactor.passthough


import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{PropsAdapter, _}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.WatchedActorTerminatedException
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.triadicsystems.examples.protocol.FlowMessages.Response
import com.triadicsystems.examples.withstageactor.passthough.IdFlowMessages.{FlowMessageWithId, InvocationWithId}
import com.triadicsystems.examples.withstageactor.stageactor.StreamToActorMessaging.StreamToActorMessage
import com.typesafe.scalalogging.LazyLogging

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait WithId {
  def id:UUID
}

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
        logger.debug(s"Step 1, 3 arrived, will throw exception")
        throw new IllegalStateException("3 is a bad number")
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
