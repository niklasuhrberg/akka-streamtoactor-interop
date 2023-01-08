package com.triadicsystems.examples.withstageactor.stageactor

import akka.actor.typed.ActorRef
import com.triadicsystems.examples.protocol.FlowMessages.{Invocation, Response}


object StreamToActorMessaging {
  trait StreamToActorMessage[+T]

  case class StreamInit[T](replyTo:ActorRef[StreamToActorMessage[T]]) extends StreamToActorMessage[T]

  case object StreamAck extends StreamToActorMessage[Nothing]

  case object StreamCompleted extends StreamToActorMessage[Nothing]

  case class StreamFailed(ex: Throwable) extends StreamToActorMessage[Nothing]

  case class StreamElementIn[T](msg: T, replyTo:ActorRef[StreamToActorMessage[T]]) extends StreamToActorMessage[T]

  case class StreamElementOut[T](msg: T) extends StreamToActorMessage[T]

  case class StreamElementOutWithAck[T](msg:T) extends StreamToActorMessage[T]
}
