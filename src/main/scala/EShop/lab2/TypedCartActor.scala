package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}

import scala.language.postfixOps
import scala.concurrent.duration._

object TypedCartActor {
//  def apply(): Behavior[TypedCartActor] = Behaviors.setup { context =>
//    context.spawn(TypedCartActor(), "")
//  }

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Event
}

class TypedCartActor {

  import TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer(context: ActorContext[TypedCartActor.Command]): Cancellable =
    context.system.scheduler
      .scheduleOnce(cartTimerDuration, () => { context.self ! ExpireCart })(context.executionContext)

  def start: Behavior[TypedCartActor.Command] = empty

  def empty: Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case AddItem(item) =>
          nonEmpty(Cart(Seq[Any](item)), scheduleTimer(context))
        case _ =>
          Behaviors.same
    }
  )

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (_, msg) =>
      msg match {
        case AddItem(item) =>
          cart.addItem(item)
          Behaviors.same
        case RemoveItem(item) =>
          if (cart.removeItem(item).size == 0) {
            timer.cancel()
            empty
          } else {
            Behaviors.same
          }
        case ExpireCart =>
          empty
        case StartCheckout =>
          timer.cancel()
          inCheckout(cart)
        case _ =>
          Behaviors.same
    }
  )

  def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) =>
      msg match {
        case ConfirmCheckoutCancelled =>
          nonEmpty(cart, scheduleTimer(context))
        case ConfirmCheckoutClosed =>
          empty
        case _ =>
          Behaviors.same
    }
  )
}
