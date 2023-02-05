package com.triadicsystems.examples.stageactorbased

import akka.actor.typed.ActorSystem

object Main extends App {

  ActorSystem[Nothing](StreamWithStageActor(), "Demosystem")
}
