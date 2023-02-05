package com.triadicsystems.examples.protocol

/**
 * Application message used in the two stage actor based examples (in the stageactorbased and passthrough packages)
 */
object FlowMessages {
  trait FlowMessage
  case class Invocation(name: String) extends FlowMessage
  case class Response(name: String, sum: Int) extends FlowMessage
}
