package com.triadicsystems.examples.passthrough

import java.util.UUID

/**
 * Application messages with an id to support the pass-through style functionality.
 */
object IdFlowMessages {
  trait FlowMessageWithId {
    def id:UUID
  }
  case class InvocationWithId(id:UUID, name: String) extends FlowMessageWithId
  case class ResponseWithId(id:UUID, name: String, sum: Int) extends FlowMessageWithId
}
