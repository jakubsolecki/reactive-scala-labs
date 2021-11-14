package EShop.lab5

import EShop.lab2.TypedCheckout
import EShop.lab3.OrderManager
import EShop.lab5.Payment.{PaymentRejected, WrappedPaymentServiceResponse}
import EShop.lab5.PaymentService.{PaymentClientError, PaymentServerError, PaymentSucceeded}
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy.Escalate
import akka.actor.typed.SupervisorStrategy.Stop
import akka.actor.typed.{ActorRef, Behavior, ChildFailed, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.StreamTcpException

import scala.concurrent.duration._
import akka.actor.typed.Terminated
import org.slf4j.event.Level

object Payment {
  sealed trait Message

  case object DoPayment extends Message

  case class WrappedPaymentServiceResponse(response: PaymentService.Response) extends Message

  sealed trait Response

  case object PaymentRejected extends Response

  val restartStrategy = SupervisorStrategy.restart.withLimit(maxNrOfRetries = 3, withinTimeRange = 1.second)

  def apply(
    method: String,
    orderManager: ActorRef[OrderManager.Command],
    checkout: ActorRef[TypedCheckout.Command]
  ): Behavior[Message] = {
    Behaviors.supervise {
      case _: PaymentClientError =>
        notifyAboutRejection(orderManager, checkout)
        Stop(loggingEnabled = true, Level.INFO)
      case _: Exception => Escalate
    }
    Behaviors
      .receive[Message]((context, msg) =>
        msg match {
          case DoPayment =>
            val payment = context.spawn(PaymentService(method, context.self.unsafeUpcast[Any]), "PaymentService")
//            context.watch(payment)
            Behaviors.same
          case WrappedPaymentServiceResponse(PaymentSucceeded) =>
            orderManager ! OrderManager.PaymentReceived
            checkout ! TypedCheckout.PaymentReceived
            Behaviors.same
        }
      )
      .receiveSignal {
        case (context, Terminated(t)) =>
          notifyAboutRejection(orderManager, checkout)
          Behaviors.same
      }
  }

  //  override val supervisorStrategy: OneForOneStrategy =
//    OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1.seconds) {
//      //      case _: PaymentServerError =>
//      //        notifyAboutRestart()
//      //        Restart
//      //      case _: PaymentServerError =>
//      //        notifyAboutRejection(orderManager, checkout)
//      //        Stop(true, Level.INFO)
//      case _: PaymentClientError =>
//        notifyAboutRejection(orderManager, checkout)
//        Stop(true, Level.INFO)
//      case _: Exception => Escalate
//    }

  // please use this one to notify when supervised actor was stoped
  private def notifyAboutRejection(
    orderManager: ActorRef[OrderManager.Command],
    checkout: ActorRef[TypedCheckout.Command]
  ): Unit = {
    orderManager ! OrderManager.PaymentRejected
    checkout ! TypedCheckout.PaymentRejected
  }
}
