package EShop.lab3

import EShop.lab2
import EShop.lab2.{TypedCartActor, TypedCheckout}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout

import scala.concurrent.{ExecutionContextExecutor, Future}
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
  case class CheckoutEvent(message: TypedCheckout.Event)                                              extends Command
  case class CartActorEvent(message: TypedCartActor.Event)                                            extends Command

  sealed trait Ack
  case object Done extends Ack //trivial ACK
}

class OrderManager {

  import OrderManager._

  var checkoutAdapter: ActorRef[TypedCheckout.Event]   = null
  var cartActorAdapter: ActorRef[TypedCartActor.Event] = null

  def start: Behavior[OrderManager.Command] = Behaviors.setup { context =>
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
          cartActor ! TypedCartActor.StartCheckout(cartActorAdapter)
          inCheckout(cartActor, sender)
        case _ =>
          context.log.info(s"Unknown message $msg in open")
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
          context.log.info(s"Unknown message $msg in inCheckout(2)")
          Behaviors.same
    }
  )

  def inCheckout(checkoutActorRef: ActorRef[TypedCheckout.Command]): Behavior[OrderManager.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case OrderManager.SelectDeliveryAndPaymentMethod(delivery, payment, sender) =>
          checkoutActorRef ! TypedCheckout.SelectDeliveryMethod(delivery)
          implicit val timeout: Timeout              = 0.5 seconds
          implicit val scheduler: Scheduler          = context.system.scheduler
          val res: Future[Any]                       = checkoutActorRef.ask(checkoutAdapter => TypedCheckout.SelectPayment(payment, checkoutAdapter))
          implicit val ctx: ExecutionContextExecutor = context.executionContext
          var p: ActorRef[Payment.Command]           = null
          res.onComplete {
//            case Success(OrderManager.ConfirmPaymentStarted(paymentRef)) =>
            case Success(TypedCheckout.PaymentStarted(paymentRef)) =>
              sender ! Done
              p = paymentRef
          }
          inPayment(p, sender)
        case _ =>
          context.log.info(s"Unknown message $msg in inCheckout(1)")
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
          context.log.info(s"Unknown message $msg in payment")
          Behaviors.same
    }
  )

  def finished: Behavior[OrderManager.Command] = Behaviors.stopped
}
