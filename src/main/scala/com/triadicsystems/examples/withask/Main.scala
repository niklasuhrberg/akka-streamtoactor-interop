package com.triadicsystems.examples.withask

import akka.actor.typed.ActorSystem

object Main extends App {

  ActorSystem[Nothing](StreamWithAsk(), "Demosystem")
}
