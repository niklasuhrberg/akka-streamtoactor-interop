package com.triadicsystems.examples.withstageactor.stageactor
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorRef, Terminated}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream._

import scala.util.Failure

class ActorRefBackpressureProcessFlowStage[T](private val targetActor: ActorRef) extends GraphStage[FlowShape[T, T]] {
  /**
   * Sends the elements of the stream to the given `ActorRef` that sends back back-pressure signal.
   * First element is always `StreamInit`, then stream is waiting for acknowledgement message
   * `ackMessage` from the given actor which means that it is ready to process
   * elements. It also requires `ackMessage` message after each stream element
   * to make backpressure work. Stream elements are wrapped inside `StreamElementIn(elem)` messages.
   *
   * The target actor can emit elements at any time by sending a `StreamElementOut(elem)` message, which will
   * be emitted downstream when there is demand. There is also a StreamElementOutWithAck(elem), that combines the
   * StreamElementOut and StreamAck message in one.
   *
   * If the target actor terminates the stage will fail with a WatchedActorTerminatedException.
   * When the stream is completed successfully a `StreamCompleted` message
   * will be sent to the destination actor.
   * When the stream is completed with failure a `StreamFailed(ex)` message will be send to the destination actor.
   *
   * Note: The author of this code originally is Fran van Meeuwen. I have only adjusted the messaging protocol due to the
   * fact that I use Akka Typed. 
   *
   * @author Frank van Meeuwen with some adaptations by Niklas Uhrberg (for akka typed)
   */
    import StreamToActorMessaging._

    val in: Inlet[T] = Inlet("ActorFlowIn")
    val out: Outlet[T] = Outlet("ActorFlowOut")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

      var firstAckReceived: Boolean = false
      var firstPullReceived: Boolean = false
      var expectingAck: Boolean = false

      def stageActorReceive(messageWithSender: (ActorRef, Any)): Unit = {
        def onAck(): Unit = {
          firstAckReceived = true
          expectingAck = false
          pullIfNeeded()
          completeStageIfNeeded()
        }

        def onElementOut(elemOut: Any): Unit = {
          val elem = elemOut.asInstanceOf[T]
          emit(out, elem)
        }

        messageWithSender match {
          case (_, StreamAck) =>
            onAck()
          case (_, StreamElementOut(elemOut)) =>
            onElementOut(elemOut)
          case (_, StreamElementOutWithAck(elemOut)) =>
            onElementOut(elemOut)
            onAck()
          case (actorRef, Failure(cause)) =>
            terminateActorAndFailStage(new RuntimeException(s"Exception during processing by actor $actorRef: ${cause.getMessage}", cause))

          case (_, Terminated(targetRef)) =>
            failStage(new WatchedActorTerminatedException("ActorRefBackpressureFlowStage", targetRef))

          case (actorRef, unexpected) =>
            terminateActorAndFailStage(new IllegalStateException(s"Unexpected message: `$unexpected` received from actor `$actorRef`."))
        }
      }
      private lazy val self = getStageActor(stageActorReceive)

      override def preStart(): Unit = {
        //initialize stage actor and watch flow actor.
        self.watch(targetActor)
        tellTargetActor(StreamInit[T](self.ref))
        expectingAck = true
      }

      setHandler(in, new InHandler {

        override def onPush(): Unit = {
          val elementIn = grab(in)
          tellTargetActor(StreamElementIn[T](elementIn, self.ref))
          expectingAck = true
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          self.unwatch(targetActor)
          tellTargetActor(StreamFailed(ex))
          super.onUpstreamFailure(ex)
        }

        override def onUpstreamFinish(): Unit = {
          if(!expectingAck) {
            unwatchAndSendCompleted()
            super.onUpstreamFinish()
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          if (!firstPullReceived) {
            firstPullReceived = true
            pullIfNeeded() //Only do the first pull
          }
        }

        override def onDownstreamFinish(cause: Throwable): Unit = {
          unwatchAndSendCompleted()
          super.onDownstreamFinish(cause)
        }
      })

      private def pullIfNeeded(): Unit = {
        if(firstAckReceived && firstPullReceived && !hasBeenPulled(in)) {
          tryPull(in)
        }
      }

      private def completeStageIfNeeded(): Unit = {
        if(isClosed(in)) {
          unwatchAndSendCompleted()
          this.completeStage() //Complete stage when in is closed, this might happen if onUpstreamFinish is called when still expecting an ack.
        }
      }

      private def unwatchAndSendCompleted(): Unit = {
        self.unwatch(targetActor)
        tellTargetActor(StreamCompleted)
      }

      private def tellTargetActor(message: Any): Unit = {
        targetActor ! message
      }

      private def terminateActorAndFailStage(ex: Throwable): Unit = {
        self.unwatch(targetActor)
        tellTargetActor(StreamFailed(ex))
        failStage(ex)
      }
    }

    override def shape: FlowShape[T, T] = FlowShape(in, out)

  }

