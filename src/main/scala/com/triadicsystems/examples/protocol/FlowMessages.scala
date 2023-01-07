package com.triadicsystems.examples.protocol

object FlowMessages {
  trait FlowMessage
  case class Invocation(name: String) extends FlowMessage
  case class Response(name: String, sum: Int) extends FlowMessage
}
