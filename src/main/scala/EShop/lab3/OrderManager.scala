package EShop.lab3

import EShop.lab2.{TypedCartActor, TypedCheckout}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout

import scala.concurrent.duration.DurationDouble
import scala.language.postfixOps
import scala.util.{Failure, Success}

object OrderManager {

  sealed trait Command
  case class AddItem(id: String, sender: ActorRef[Ack])                                               extends Command
  case class RemoveItem(id: String, sender: ActorRef[Ack])                                            extends Command
  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String, sender: ActorRef[Ack]) extends Command
  case class Buy(sender: ActorRef[Ack])                                                               extends Command
  case class Pay(sender: ActorRef[Ack])                                                               extends Command
  case class ConfirmCheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command])                     extends Command
  case class ConfirmPaymentStarted(paymentRef: ActorRef[Payment.Command])                             extends Command
  case object ConfirmPaymentReceived                                                                  extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class OrderManager {

  import OrderManager._

  def start: Behavior[OrderManager.Command] = Behaviors.setup { context =>
    val cartActor = context.spawn(TypedCartActor(), "cartActor")
    open(cartActor)
  }

  def uninitialized: Behavior[OrderManager.Command] = start

  def open(cartActor: ActorRef[TypedCartActor.Command]): Behavior[OrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(id, sender) =>
          cartActor ! TypedCartActor.AddItem(id)
          sender ! Done
          Behaviors.same
        case RemoveItem(id, sender) =>
          cartActor ! TypedCartActor.RemoveItem(id)
          sender ! Done
          Behaviors.same
        case Buy(sender) =>
          cartActor ! TypedCartActor.StartCheckout(context.self)
          inCheckout(cartActor, sender)
        case _ =>
          context.log.info(s"Unknown message $msg")
          Behaviors.same
    }
  )

  def inCheckout(
    cartActorRef: ActorRef[TypedCartActor.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[OrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case OrderManager.ConfirmCheckoutStarted(checkoutRef) =>
          senderRef ! Done
          inCheckout(checkoutRef)
        case _ =>
          context.log.info(s"Unknown message $msg")
          Behaviors.same
    }
  )

  def inCheckout(checkoutActorRef: ActorRef[TypedCheckout.Command]): Behavior[OrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case OrderManager.SelectDeliveryAndPaymentMethod(delivery, payment, sender) =>
          checkoutActorRef ! TypedCheckout.SelectDeliveryMethod(delivery)
          implicit val timeout: Timeout = 0.5 seconds
          implicit val scheduler = context.system.scheduler
          val res = checkoutActorRef.ask(ref => TypedCheckout.SelectPayment(payment, ref))
          implicit val ctx = context.executionContext
          var p: ActorRef[Payment.Command] = null
          res.onComplete {
            case Success(OrderManager.ConfirmPaymentStarted(paymentRef)) =>
              sender ! Done
              p = paymentRef
            case Failure(_) => Behaviors.same
          }
          inPayment(p, sender)
        case _ =>
          context.log.info(s"Unknown message $msg")
          Behaviors.same
    }
  )

  def inPayment(
    paymentActorRef: ActorRef[Payment.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[OrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case Pay(sender) =>
          sender ! Done
          Behaviors.same
        case ConfirmPaymentReceived =>
          senderRef ! Done
          finished
        case _ =>
          context.log.info(s"Unknown message $msg, pay")
          Behaviors.same
    }
  )

  def finished: Behavior[OrderManager.Command] = Behaviors.stopped
}
