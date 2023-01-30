package com.triadicsystems.examples.withstageactor.passthough

import akka.actor.typed.ActorSystem

object MainStageActorWithPassthrough extends App {

  ActorSystem[Nothing](PassthroughStreamWithStageActor(), "Demosystem")
}
