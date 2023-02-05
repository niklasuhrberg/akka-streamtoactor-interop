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

  var counter = 6 // Change the initial value to study the behavior in the actor fails (1), or causes a timeout (6)
  def apply(): Behavior[ProcessMessage] = Behaviors.receiveMessage {
    case Invocation(name, replyTo) =>
      counter += 1
      if(counter == 3) throw new IllegalArgumentException("Three is a bad number from target actor")
      if(counter == 8) {
        logger.debug(s"Received Invocation($name) with counter == 8, will now sleep for 10 seconds to cause a timeout")
        Thread.sleep(10000)
        logger.debug(s"Sleeping is over and will now reply")
        replyTo ! Response(name, Random.nextInt(100))
        Behaviors.same
      } else {
        logger.debug(s"Target actor received Invocation($name)")
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
