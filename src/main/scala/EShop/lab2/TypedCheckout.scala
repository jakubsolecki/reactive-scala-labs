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
  case object StartCheckout                                                   extends Command
  case class SelectDeliveryMethod(method: String)                             extends Command
  case object CancelCheckout                                                  extends Command
  case object ExpireCheckout                                                  extends Command
  case class SelectPayment(payment: String, orderManagerRef: ActorRef[Event]) extends Command
  case object ExpirePayment                                                   extends Command
  case object ConfirmPaymentReceived                                          extends Command
  case object PaymentReceived                                                 extends Command
  case object PaymentRejected                                                 extends Command

  sealed trait Event
  case object CheckOutClosed                                       extends Event
  case class PaymentStarted(paymentRef: ActorRef[Payment.Command]) extends Event
  case object CheckoutStarted                                      extends Event
  case object CheckoutCancelled                                    extends Event
  case class DeliveryMethodSelected(method: String)                extends Event

  sealed abstract class State(val timerOpt: Option[Cancellable])
  case object WaitingForStart                           extends State(None)
  case class SelectingDelivery(timer: Cancellable)      extends State(Some(timer))
  case class SelectingPaymentMethod(timer: Cancellable) extends State(Some(timer))
  case object Closed                                    extends State(None)
  case object Cancelled                                 extends State(None)
  case class ProcessingPayment(timer: Cancellable)      extends State(Some(timer))

  def apply(cartActor: ActorRef[TypedCartActor.Command]): Behavior[TypedCheckout.Command] =
    Behaviors.setup { _ =>
      val actor = new TypedCheckout(cartActor)
      actor.start
    }
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
    context.scheduleOnce(timerDuration, context.self, command)

  def start: Behavior[TypedCheckout.Command] =
    Behaviors.setup(context => selectingDelivery(scheduleTimer(context, checkoutTimerDuration, ExpireCheckout)))

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receive((context, msg) =>
      msg match {
        case ExpireCheckout          => cancelled
        case CancelCheckout          => cancelled
        case SelectDeliveryMethod(_) => selectingPaymentMethod(timer)
        case _                       => Behaviors.same
      }
    )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receive((context, msg) =>
      msg match {
        case ExpireCheckout => cancelled
        case CancelCheckout => cancelled
        case SelectPayment(payment: String, orderManagerRef: ActorRef[Any]) =>
          timer.cancel()
          val paymentRef = context.spawn(Payment(payment, orderManagerRef, context.self), "payment")
          orderManagerRef ! PaymentStarted(paymentRef)
          processingPayment(scheduleTimer(context, paymentTimerDuration, ExpirePayment))
        case _ => Behaviors.same
      }
    )

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] =
    Behaviors.receive((context, msg) =>
      msg match {
        case ExpirePayment  => cancelled
        case CancelCheckout => cancelled
        case ConfirmPaymentReceived =>
          timer.cancel()
          cartActor ! TypedCartActor.ConfirmCheckoutClosed
          closed
        case _ => Behaviors.same
      }
    )

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.stopped

  def closed: Behavior[TypedCheckout.Command] = Behaviors.stopped
}
