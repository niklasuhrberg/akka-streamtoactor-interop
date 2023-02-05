package com.triadicsystems.examples.stageactorbased

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.triadicsystems.examples.protocol.FlowMessages.{FlowMessage, Invocation, Response}
import com.triadicsystems.examples.protocol.StreamToActorMessaging.StreamToActorMessage
import com.typesafe.scalalogging.LazyLogging

import scala.util.Random
import com.triadicsystems.examples.protocol.StreamToActorMessaging._
/*
  Processes Invocation messages by responding with Response messages, with the twist of not responding for the two
  first Invocation messaged and after that responding twice.
  This shows a possibility that is not available in the ask based approach.
  The messages are tunneled through the StreamToActorMessage protocol.
 */
object TargetActor extends LazyLogging {
  var counter = 0
  def apply():Behavior[StreamToActorMessage[FlowMessage]] = Behaviors.receiveMessage[StreamToActorMessage[FlowMessage]] {
    case StreamInit(replyTo) =>
      logger.debug("Received StreamInit, will signal demand by responding ack")
      replyTo ! StreamAck
      Behaviors.same

    case StreamElementIn(msg:Invocation, replyTo)  =>
      counter += 1
      if(counter == 5) throw new IllegalStateException("5 is a bad number from target actor")
      logger.debug(s"Received StreamElementIn with $msg")
      if(counter > 2) {
        replyTo ! StreamElementOut(Response(msg.name, Random.nextInt(100)))
        replyTo ! StreamElementOut(Response(msg.name, Random.nextInt(100)))
      }
      replyTo ! StreamAck
      Behaviors.same

    case StreamFailed(throwable) =>
      logger.debug(s"Received StreamFailed($throwable), will cleanup and stop")
      Behaviors.stopped
    case StreamCompleted =>
      logger.debug(s"Recevied StreamCompleted, will cleanup and stop")
      Behaviors.stopped

  }

}
