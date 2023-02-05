package com.triadicsystems.examples.askbased

import akka.actor.typed.ActorSystem

object MainAskBased extends App {

  ActorSystem[Nothing](StreamWithAsk(), "Demosystem")
}
