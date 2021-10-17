package EShop.lab2

import EShop.lab2.Checkout._
import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

object Checkout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckOutClosed                   extends Event
  case class PaymentStarted(payment: ActorRef) extends Event

}

class Checkout extends Actor {

  private val scheduler = context.system.scheduler
  private val log       = Logging(context.system, this)

  val checkoutTimerDuration = 1 seconds
  val paymentTimerDuration  = 1 seconds

  def receive: Receive = LoggingReceive {
    case StartCheckout =>
      context become selectingDelivery(
        scheduler.scheduleOnce(checkoutTimerDuration, self, ExpireCheckout)(context.dispatcher, self)
      )
  }

  def selectingDelivery(timer: Cancellable): Receive = LoggingReceive {
    case ExpireCheckout =>
      context become cancelled

    case CancelCheckout =>
      context become cancelled

    case SelectDeliveryMethod(_) =>
      context become selectingPaymentMethod(timer)
  }

  def selectingPaymentMethod(timer: Cancellable): Receive = LoggingReceive {
    case ExpireCheckout =>
      context become cancelled

    case CancelCheckout =>
      context become cancelled

    case SelectPayment(_) =>
      timer.cancel()
      context become processingPayment(
        scheduler.scheduleOnce(paymentTimerDuration, self, ExpirePayment)(context.dispatcher, self)
      )
  }

  def processingPayment(timer: Cancellable): Receive = LoggingReceive {
    case ExpirePayment =>
      context become cancelled

    case CancelCheckout =>
      context become cancelled

    case ConfirmPaymentReceived =>
      timer.cancel()
      context become closed
  }

  def cancelled: Receive = LoggingReceive {
    case _ => None
  }

  def closed: Receive = LoggingReceive {
    case _ => None
  }
}

object CheckoutApp extends App {
  val actorSystem = ActorSystem("checkoutSystem")
  val checkout    = actorSystem.actorOf(Props[Checkout], "checkout")

  import Checkout._

  checkout ! StartCheckout
  checkout ! SelectDeliveryMethod("DHL")
  checkout ! SelectPayment("BLIK")
  checkout ! ConfirmPaymentReceived
  sys.exit()

}
