package com.triadicsystems.examples.askbased

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.scalalogging.LazyLogging

import scala.util.Random

object TargetActor extends LazyLogging {

  trait ProcessMessage

  case class Invocation(name: String, replyTo: ActorRef[ProcessMessage]) extends ProcessMessage
  case object StreamCompleted extends ProcessMessage
  case object StreamFailed extends ProcessMessage
  case class Response(name: String, sum: Int) extends ProcessMessage

  var counter = 9 // Change the initial value to study the behavior in the actor fails (1), or causes a timeout (6)
  def apply(): Behavior[ProcessMessage] = Behaviors.receiveMessage {
    case Invocation(name, replyTo) =>
      counter += 1
      if(counter == 3) throw new IllegalArgumentException("Artifical exception")
      if(counter == 8) {
        logger.debug(s"Received Invocation($name) with counter == 8, will now sleep for 10 seconds")
        Thread.sleep(10000)
        logger.debug(s"Sleeping is over and will now reply")
        replyTo ! Response(name, Random.nextInt(100))
        Behaviors.same
      } else {
        val toSleep = Random.nextInt(5) * 100
        logger.debug(s"Received Invocation($name), will sleep $toSleep before responding")
        Thread.sleep(toSleep)
        replyTo ! Response(name, Random.nextInt(100))
        Behaviors.same
      }
    case StreamCompleted =>
      logger.debug("Received StreamCompleted message, will cleanup and stop.")
      Behaviors.stopped
    case StreamFailed =>
      logger.debug("Received StreamFailed message, will cleanup and stop.")
      Behaviors.stopped
  }
}
