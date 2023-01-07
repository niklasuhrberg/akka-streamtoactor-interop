package com.triadicsystems.examples.withstageactor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.triadicsystems.examples.protocol.FlowMessages.{FlowMessage, Response}
import com.triadicsystems.examples.withstageactor.stageactor.ActorRefBackpressureProcessFlowStage
import com.triadicsystems.examples.withstageactor.stageactor.StreamToActorMessaging.StreamToActorMessage
import com.typesafe.scalalogging.LazyLogging

import scala.util.Random
import com.triadicsystems.examples.withstageactor.stageactor.StreamToActorMessaging._
object TargetActor extends LazyLogging {
  var counter = 0
  def apply():Behavior[StreamToActorMessage] = Behaviors.receiveMessage[StreamToActorMessage] {
    case StreamInit(replyTo) =>
      logger.debug("Received StreamInit, will signal demand by responding ack")
      replyTo ! StreamAck
      Behaviors.same

    case StreamElementIn(msg, replyTo)  =>
      counter += 1
      if(counter ==10) throw new IllegalStateException("Artifical exception")
      val toSleep = Random.nextInt(5) * 100
      logger.debug(s"Received StreamElementIn with $msg, will sleep $toSleep before responding")
      Thread.sleep(toSleep)
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
