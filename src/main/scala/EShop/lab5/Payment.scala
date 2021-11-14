package EShop.lab5

import EShop.lab2.TypedCheckout
import EShop.lab3.OrderManager
import EShop.lab5.PaymentService.PaymentSucceeded
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object Payment {
  sealed trait Message
  case object DoPayment                                                       extends Message
  case class WrappedPaymentServiceResponse(response: PaymentService.Response) extends Message
//  case object PaymentServiceError                                             extends Message

  sealed trait Response
  case object PaymentRejected extends Response

  val restartStrategy = SupervisorStrategy.restart.withLimit(maxNrOfRetries = 3, withinTimeRange = 1.second)

  def apply(
    method: String,
    orderManager: ActorRef[OrderManager.Command],
    checkout: ActorRef[TypedCheckout.Command]
  ): Behavior[Message] = {
    Behaviors
      .receive[Message]((context, msg) =>
        msg match {
          case DoPayment =>
            val adapter = context.messageAdapter[PaymentService.Response] {
              case response @ PaymentService.PaymentSucceeded => WrappedPaymentServiceResponse(response)
            }
            val paymentService = Behaviors
              .supervise(PaymentService(method, adapter))
              .onFailure(restartStrategy)
            val paymentServiceRef = context.spawnAnonymous(paymentService)
//            context.watchWith(paymentServiceRef, PaymentServiceError)
            context.watch(paymentServiceRef)
            Behaviors.same

          case WrappedPaymentServiceResponse(PaymentSucceeded) =>
            orderManager ! OrderManager.ConfirmPaymentReceived
            checkout ! TypedCheckout.PaymentReceived
            Behaviors.same

//          case PaymentServiceError =>
//            notifyAboutRejection(orderManager, checkout)
//            Behaviors.same
        }
      )
      .receiveSignal {
        case (context, Terminated(t)) =>
          notifyAboutRejection(orderManager, checkout)
          Behaviors.same
      }
  }

  // please use this one to notify when supervised actor was stopped
  private def notifyAboutRejection(
    orderManager: ActorRef[OrderManager.Command],
    checkout: ActorRef[TypedCheckout.Command]
  ): Unit = {
    orderManager ! OrderManager.PaymentRejected
    checkout ! TypedCheckout.PaymentRejected
  }
}
