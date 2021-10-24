package EShop.lab2

import EShop.lab2
import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.language.postfixOps
import scala.concurrent.duration._
import EShop.lab3.{OrderManager, Payment}

object TypedCheckout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                                                                  extends Command
  case class SelectDeliveryMethod(method: String)                                            extends Command
  case object CancelCheckout                                                                 extends Command
  case object ExpireCheckout                                                                 extends Command
  case class SelectPayment(payment: String, orderManagerRef: ActorRef[OrderManager.Command]) extends Command
  case object ExpirePayment                                                                  extends Command
  case object ConfirmPaymentReceived                                                         extends Command

  sealed trait Event
  case object CheckOutClosed                           extends Event
  case class PaymentStarted(paymentRef: ActorRef[Any]) extends Event

  def apply(cartActor: ActorRef[TypedCartActor.Command]): Behavior[TypedCheckout.Command] = Behaviors.setup(
    context => {
      val actor = new TypedCheckout(cartActor)
      actor.start
    }
  )
}

class TypedCheckout(
  cartActor: ActorRef[TypedCartActor.Command]
) {
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

  def start: Behavior[TypedCheckout.Command] = Behaviors.setup(
    context => selectingDelivery(scheduleTimer(context, checkoutTimerDuration, ExpireCheckout))
  )

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ExpireCheckout               => cancelled
        case CancelCheckout               => cancelled
        case SelectDeliveryMethod(method) => selectingPaymentMethod(timer)
        case _ =>
          context.log.info(s"Unknown message $msg")
          Behaviors.same
    }
  )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ExpireCheckout => cancelled
        case CancelCheckout => cancelled
        case SelectPayment(payment: String, orderManagerRef: ActorRef[OrderManager.Command]) =>
          timer.cancel()
          val paymentRef = context.spawn(Payment(payment, orderManagerRef, context.self), "payment")
          orderManagerRef ! OrderManager.ConfirmPaymentStarted(paymentRef)
          processingPayment(scheduleTimer(context, paymentTimerDuration, ExpirePayment))
        case _ =>
          context.log.info(s"Unknown message $msg")
          Behaviors.same
    }
  )

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ExpirePayment  => cancelled
        case CancelCheckout => cancelled
        case ConfirmPaymentReceived =>
          timer.cancel()
          cartActor ! TypedCartActor.ConfirmCheckoutClosed
          closed
        case _ =>
          context.log.info(s"Unknown message $msg")
          Behaviors.same
    }
  )

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.stopped

  def closed: Behavior[TypedCheckout.Command] = Behaviors.stopped
}
