package EShop.lab3

import EShop.lab2.{TypedCartActor, TypedCheckout}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.language.postfixOps

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
  case class CheckoutEvent(message: TypedCheckout.Event)                                              extends Command
  case class CartActorEvent(message: TypedCartActor.Event)                                            extends Command
  case object PaymentReceived                                                                         extends Command
  case object PaymentRejected                                                                         extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class OrderManager {

  import OrderManager._

  var checkoutAdapter: ActorRef[TypedCheckout.Event]   = null
  var cartActorAdapter: ActorRef[TypedCartActor.Event] = null

  def start: Behavior[OrderManager.Command] =
    Behaviors.setup { context =>
      checkoutAdapter = context.messageAdapter {
        case TypedCheckout.PaymentStarted(paymentRef) => ConfirmPaymentStarted(paymentRef)
      }
      cartActorAdapter = context.messageAdapter {
        case TypedCartActor.CheckoutStarted(checkoutRef) => ConfirmCheckoutStarted(checkoutRef)
      }
      val cartActor = context.spawn(TypedCartActor(), "cartActor")
      open(cartActor)
    }

  def uninitialized: Behavior[OrderManager.Command] = start

  def open(cartActor: ActorRef[TypedCartActor.Command]): Behavior[OrderManager.Command] =
    Behaviors.receive((context, msg) =>
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
          inCheckout(cartActor, sender)
        case _ => Behaviors.same
      }
    )

  def inCheckout(
    cartActorRef: ActorRef[TypedCartActor.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[OrderManager.Command] =
    Behaviors.setup { _ =>
      cartActorRef ! TypedCartActor.StartCheckout(cartActorAdapter)
      Behaviors.receive((context, msg) =>
        msg match {
          case OrderManager.ConfirmCheckoutStarted(checkoutRef) =>
            senderRef ! Done
            inCheckout(checkoutRef)
          case _ => Behaviors.same
        }
      )
    }

  def inCheckout(checkoutActorRef: ActorRef[TypedCheckout.Command]): Behavior[OrderManager.Command] =
    Behaviors.receive((context, msg) =>
      msg match {
        case OrderManager.SelectDeliveryAndPaymentMethod(delivery, payment, sender) =>
          checkoutActorRef ! TypedCheckout.SelectDeliveryMethod(delivery)
          checkoutActorRef ! TypedCheckout.SelectPayment(payment, checkoutAdapter)
          inPayment(sender)
        case _ => Behaviors.same
      }
    )

  def inPayment(senderRef: ActorRef[Ack]): Behavior[OrderManager.Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case ConfirmPaymentStarted(paymentActorRef) =>
          inPayment(paymentActorRef, senderRef)
        case _ => Behaviors.same
      }
    }

  def inPayment(
    paymentActorRef: ActorRef[Payment.Command],
    senderRef: ActorRef[Ack]
  ): Behavior[OrderManager.Command] =
    Behaviors.setup { _ =>
      senderRef ! Done
      Behaviors.receive((context, msg) =>
        msg match {
          case Pay(sender) =>
            paymentActorRef ! Payment.DoPayment
            inPayment(paymentActorRef, sender)
          case ConfirmPaymentReceived =>
            senderRef ! Done
            finished
          case _ => Behaviors.same
        }
      )
    }

  def finished: Behavior[OrderManager.Command] = Behaviors.stopped
}
