package com.triadicsystems.examples.withstageactor.passthough

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.triadicsystems.examples.protocol.FlowMessages.{FlowMessage, Invocation, Response}
import com.triadicsystems.examples.withstageactor.passthough.IdFlowMessages.{FlowMessageWithId, InvocationWithId, ResponseWithId}
import com.triadicsystems.examples.withstageactor.stageactor.StreamToActorMessaging._
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
      if(counter ==10) throw new IllegalStateException("Artifical exception")
      val toSleep = Random.nextInt(5) * 100
      logger.debug(s"Received StreamElementIn with $msg, will sleep $toSleep before responding")
      Thread.sleep(toSleep)
      if(counter > 2) {
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
