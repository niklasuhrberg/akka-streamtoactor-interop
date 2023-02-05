package com.triadicsystems.examples.protocol

import akka.actor.typed.ActorRef

/**
 * The streams control messaging protocol used by the two stage actor based sample applications.
 * The protocol is generic to allow reuse in different applications. Note the use of the type Nothing in the case objects.
 *
 */
object StreamToActorMessaging {
  trait StreamToActorMessage[+T]

  /**
   * This message is sent to the target actor as the very first message.
   * @param replyTo An actor ref to the stage actor in the stage actor based stream component (a custom GraphStage)
   * @tparam T
   */
  case class StreamInit[T](replyTo: ActorRef[StreamToActorMessage[T]]) extends StreamToActorMessage[T]

  /**
   * Sent from the target actor the the Akka stream to signal demand.
   */
  case object StreamAck extends StreamToActorMessage[Nothing]

  /**
   * Sent from the stream to the target actor signalling the completion. After this it makes no sense to send more messaged
   * to the stream stage actor.
   */
  case object StreamCompleted extends StreamToActorMessage[Nothing]
  /**
   * Sent from the stream to the target actor signalling the failure of the stream. After this it makes no sense to send more messaged
   * to the stream stage actor.
   */

  case class StreamFailed(ex: Throwable) extends StreamToActorMessage[Nothing]

  /**
   * Tunnels an application message (T) to the target actor. To receive more messages StreamAck must be sent the the stream.
   * @param msg
   * @param replyTo
   * @tparam T
   */
  case class StreamElementIn[T](msg: T, replyTo: ActorRef[StreamToActorMessage[T]]) extends StreamToActorMessage[T]

  /**
   * Tunnels an application message (T) to the stream which will have this message emitted down-stream.
   * @param msg
   * @tparam T
   */
  case class StreamElementOut[T](msg: T) extends StreamToActorMessage[T]

  /**
   * Convenience method combining StreamElementOut with a subsequent StreamAck
   * @param msg
   * @tparam T
   */
  case class StreamElementOutWithAck[T](msg: T) extends StreamToActorMessage[T]
}
