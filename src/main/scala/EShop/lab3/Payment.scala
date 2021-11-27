package EShop.lab3

import EShop.lab2.TypedCheckout
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  def apply(
    method: String,
    orderManager: ActorRef[Any],
    checkout: ActorRef[TypedCheckout.Command]
  ): Behavior[Payment.Command] =
    Behaviors.setup { _ =>
      val payment = new Payment(method, orderManager, checkout)
      payment.start
    }
}

class Payment(
  method: String,
  orderManager: ActorRef[Any],
  checkout: ActorRef[TypedCheckout.Command]
) {

  import Payment._

  def start: Behavior[Payment.Command] =
    Behaviors.receive((_, msg) =>
      msg match {
        case DoPayment =>
          orderManager ! OrderManager.ConfirmPaymentReceived
          checkout ! TypedCheckout.ConfirmPaymentReceived
          Behaviors.stopped
      }
    )

}
