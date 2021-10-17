package EShop.lab2
import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import akka.event.{Logging, LoggingReceive}

import java.lang.Thread.sleep
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor {

  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 5 seconds

  private def scheduleTimer: Cancellable =
    context.system.scheduler.scheduleOnce(cartTimerDuration) {
      self ! ExpireCart
    }

  def receive: Receive = LoggingReceive {
    case AddItem(item) =>
      context become nonEmpty(Cart(Seq[Any](item)), scheduleTimer)
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item) =>
      context become nonEmpty(Cart(Seq[Any](item)), scheduleTimer)
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = LoggingReceive {
    case AddItem(item) =>
      cart.addItem(item)

    case RemoveItem(item) =>
      if (cart.removeItem(item).size == 0) {
        timer.cancel()
        context become empty
      }

    case StartCheckout =>
      timer.cancel()
      context become inCheckout(cart)

    case ExpireCart =>
      context become empty
  }

  def inCheckout(cart: Cart): Receive = LoggingReceive {
    case ConfirmCheckoutCancelled =>
      context become nonEmpty(cart, scheduleTimer)

    case ConfirmCheckoutClosed =>
      context become empty
  }
}

object CartActorApp extends App {
  val actorSystem = ActorSystem("cartSystem")
  val cartActor   = actorSystem.actorOf(Props[CartActor], "cartActor")

  import CartActor._

  cartActor ! AddItem("Battlefield 2042 PC")
  cartActor ! StartCheckout
  cartActor ! ConfirmCheckoutCancelled
  cartActor ! AddItem("Bucket")
  Thread.sleep(6000)
  actorSystem.stop(cartActor)
  sys.exit()
}
