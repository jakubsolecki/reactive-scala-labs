package EShop.lab5

import EShop.lab5.Payment.Message
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.pattern.pipe

import scala.util.{Failure, Success}

object PaymentService {

  sealed trait Response
  case object PaymentSucceeded extends Response

  case class PaymentClientError() extends Exception
  case class PaymentServerError() extends Exception

  // actor behavior which needs to be supervised
  // use akka.http.scaladsl.Http to make http based payment request
  // use getUri method to obtain url
  def apply(
    method: String,
//    payment: ActorRef[Response]
    payment: ActorRef[Any]
  ): Behavior[HttpResponse] =
    Behaviors.setup { context =>
      implicit val executionContext = context.executionContext
      val http                      = Http(context.system)
//      val uri = getURI(method)
      val result = http.singleRequest(HttpRequest(uri = getURI(method)))
      context.pipeToSelf(result) {
        case Success(value) => value
        case Failure(e)     => throw e
      }
//        Behaviors.receive[HttpResponse] ((context, msg) =>
//          msg match {
//            case HttpResponse(code, _, _, _) => code.intValue() match {
//              case 200 => payment ! PaymentSucceeded
//              Behaviors.stopped
//              case 400 | 404 => throw new PaymentClientError
//              case 408 | 418 | 500 => throw new PaymentServerError
//            }
//          }
//        )
      Behaviors.receiveMessage {
        case resp @ HttpResponse(code, _, _, _) =>
          code.intValue() match {
            case 200 =>
              payment ! PaymentSucceeded
              Behaviors.stopped
            case 400 | 404       => throw new PaymentClientError
            case 408 | 418 | 500 => throw new PaymentServerError
          }
      }
    }

  // remember running PymentServiceServer() before trying payu based payments
  private def getURI(method: String) =
    method match {
      case "payu"   => "http://127.0.0.1:8080"
      case "paypal" => s"http://httpbin.org/status/408"
      case "visa"   => s"http://httpbin.org/status/200"
      case _        => s"http://httpbin.org/status/404"
    }
}
