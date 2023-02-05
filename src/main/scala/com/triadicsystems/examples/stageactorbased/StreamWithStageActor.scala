package com.triadicsystems.examples.stageactorbased

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, Behavior}
import akka.stream.WatchedActorTerminatedException
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.triadicsystems.examples.protocol.FlowMessages.{FlowMessage, Invocation, Response}
import com.triadicsystems.examples.protocol.StreamToActorMessaging.StreamToActorMessage
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
/*
This stream showcases the ask based interoperability with the following scenarios.

1. Happy flow: Processes 3 elements each sending an Invocation to the target actor and receiving a no Response messages
   for the first two and then two Response messages for the third.
    Some logging is done enabling you to study the behaviour.
    Note that when the stream completes a StreamCompleted message is sent to the target actor.
2. Failure in the stream. Change the "case 4" to e.g. "case 2" and watch the target actor get the StreamFailed message
    ending the lifecycle. (This is logged with "[Received StreamFailed({}), will cleanup and stop]"
3. Failure in the target actor. Change the var counter to e.g. 3 to study two elements be emitted before the stream
   recovers from akka.stream.WatchedActorTerminatedException and finishes with Response(The end,100).
   You can also comment out the recover section to have the stream end with failure.
   Note that when the target actor causes failure it will not get the StreamFailed message because that would not make
   sense.

   Note that in this stream using the stage actor based approach there is no failure scenario caused by a timeout since
   there can be no timeout.
 */
object StreamWithStageActor extends LazyLogging {

  implicit val timeout = Timeout(5.seconds)

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
    implicit val system: ActorSystem[_] = ctx.system
    import system.executionContext

    val targetActor = ctx.actorOf(PropsAdapter[StreamToActorMessage[FlowMessage]](TargetActor()))
    val actorFlow: Flow[FlowMessage, FlowMessage, NotUsed] =
      Flow.fromGraph(new ActorRefBackpressureProcessFlowStage[FlowMessage](targetActor))
        val value = Source(1 to 3).map {
          case 4 =>
            logger.debug(s"Step 1, 2 arrived, will throw exception")
            throw new IllegalStateException("2 is a bad number")
          case other =>
            logger.debug(s"Step 1 before the actorFlow with value $other")
            Invocation(s"Name$other")
        }
          .via(actorFlow)
          // Comment out the recover section to study the stream ending with failure instead of successful completion
          .recover { case e: WatchedActorTerminatedException =>
            logger.debug(s"Recovering from $e")
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
