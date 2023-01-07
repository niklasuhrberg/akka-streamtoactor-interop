package com.triadicsystems.examples.withstageactor

import akka.actor.typed.ActorSystem

object MainStageActor extends App {

  ActorSystem[Nothing](StreamWithStageActor(), "Demosystem")
}
