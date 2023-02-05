package com.triadicsystems.examples.passthrough

import akka.actor.typed.ActorSystem

object MainPassThrough extends App {

  ActorSystem[Nothing](PassthroughStreamWithStageActor(), "Demosystem")
}
