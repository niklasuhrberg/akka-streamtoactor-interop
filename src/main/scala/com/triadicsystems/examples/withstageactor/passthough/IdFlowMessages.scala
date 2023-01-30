package com.triadicsystems.examples.withstageactor.passthough

import java.util.UUID

object IdFlowMessages {
  trait FlowMessageWithId {
    def id:UUID
  }
  case class InvocationWithId(id:UUID, name: String) extends FlowMessageWithId
  case class ResponseWithId(id:UUID, name: String, sum: Int) extends FlowMessageWithId
}
