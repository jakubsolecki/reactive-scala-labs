package EShop.lab2

import EShop.lab2
import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.language.postfixOps
import scala.concurrent.duration._

object TypedCheckout {

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
  case object CheckOutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef[Any]) extends Event
}

class TypedCheckout {
  import TypedCheckout._

  val checkoutTimerDuration: FiniteDuration = 1 seconds
  val paymentTimerDuration: FiniteDuration  = 1 seconds

  private def scheduleTimer(
    context: ActorContext[lab2.TypedCheckout.Command],
    timerDuration: FiniteDuration,
    command: Command
  ): Cancellable =
    context.system.scheduler.scheduleOnce(timerDuration, () => {
      context.self ! command
    })(context.executionContext)

  def start: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case StartCheckout =>
          selectingDelivery(scheduleTimer(context, checkoutTimerDuration, ExpireCheckout))
    }
  )

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, msg) =>
      msg match {
        case ExpireCheckout =>
          cancelled
        case CancelCheckout =>
          cancelled
        case SelectDeliveryMethod(_) =>
          selectingPaymentMethod(timer)
    }
  )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ExpireCheckout =>
          cancelled
        case CancelCheckout =>
          cancelled
        case SelectPayment(_) =>
          timer.cancel()
          processingPayment(scheduleTimer(context, paymentTimerDuration, ExpirePayment))
    }
  )

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, msg) =>
      msg match {
        case ExpirePayment =>
          cancelled
        case CancelCheckout =>
          cancelled
        case ConfirmPaymentReceived =>
          timer.cancel()
          closed
    }
  )

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, msg) =>
      msg match {
        case _ => Behaviors.same
    }
  )

  def closed: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, msg) =>
      msg match {
        case _ => Behaviors.same
    }
  )

}
