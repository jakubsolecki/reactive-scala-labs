package EShop.lab6.cluster

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RequestCounter {
  sealed trait TopicMessage
  case object ProductCatalogRequest extends TopicMessage

  sealed trait Command
  case class ProductRequestsCount(replyTo: ActorRef[Int]) extends Command
  case object IncrementCounter                            extends Command
}

object RequestCounterApp extends App {
  import RequestCounter._

  private val config = ConfigFactory.load()

  val system = ActorSystem[Command](
    RequestCounterActor(),
    "ProductCatalog",
    config
  )

  Await.ready(system.whenTerminated, Duration.Inf)
}

object RequestCounterActor {
  import RequestCounter._

  val RequestCounterServiceKey = ServiceKey[Command]("RequestCounter")

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.register(RequestCounterServiceKey, context.self)
      val topic = context.spawn(RequestCounterTopic(), "RequestCounterTopic")
      val adapter = context.messageAdapter[TopicMessage] {
        case ProductCatalogRequest => IncrementCounter
      }

      topic ! Topic.Subscribe(adapter)
      countRequests(0)
    }

  def countRequests(state: Int): Behavior[Command] =
    Behaviors.receiveMessage {
      case IncrementCounter =>
        countRequests(state + 1)
      case ProductRequestsCount(replyTo) =>
        replyTo ! state
        Behaviors.same
    }
}

object RequestCounterTopic {
  import RequestCounter._

  def apply(): Behavior[Topic.Command[TopicMessage]] =
    Topic[TopicMessage]("request-counter")
}
