package com.triadicsystems.examples.withstageactor.stageactor

import akka.actor.typed.ActorRef
import com.triadicsystems.examples.protocol.FlowMessages.{Invocation, Response}


object StreamToActorMessaging {
  trait StreamToActorMessage

  case class StreamInit(replyTo:ActorRef[StreamToActorMessage]) extends StreamToActorMessage

  case object StreamAck extends StreamToActorMessage

  case object StreamCompleted extends StreamToActorMessage

  case class StreamFailed(ex: Throwable) extends StreamToActorMessage

  case class StreamElementIn(msg: Invocation, replyTo:ActorRef[StreamToActorMessage]) extends StreamToActorMessage

  case class StreamElementOut(msg: Response) extends StreamToActorMessage

  case class StreamElementOutWithAck(msg:Response) extends StreamToActorMessage
}
