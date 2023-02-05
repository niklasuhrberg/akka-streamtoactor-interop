package com.triadicsystems.examples.passthrough

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.triadicsystems.examples.passthrough.IdFlowMessages.{FlowMessageWithId, InvocationWithId, ResponseWithId}
import com.triadicsystems.examples.protocol.StreamToActorMessaging._
import com.typesafe.scalalogging.LazyLogging

import scala.util.Random

object PassthroughTargetActor extends LazyLogging {
  var counter = 0
  def apply():Behavior[StreamToActorMessage[FlowMessageWithId]] = Behaviors.receiveMessage[StreamToActorMessage[FlowMessageWithId]] {
    case StreamInit(replyTo) =>
      logger.debug("Received StreamInit, will signal demand by responding ack")
      replyTo ! StreamAck
      Behaviors.same

    case StreamElementIn(msg:InvocationWithId, replyTo)  =>
      counter += 1
      if(counter ==5) throw new IllegalStateException("5 is a bad number from target actor")
      if(counter <= 2) {
        replyTo ! StreamElementOut(ResponseWithId(msg.id, msg.name, Random.nextInt(100)))
      } else {
        replyTo ! StreamElementOut(ResponseWithId(msg.id, msg.name, Random.nextInt(100)))
        replyTo ! StreamElementOut(ResponseWithId(msg.id, msg.name, Random.nextInt(100)))
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
