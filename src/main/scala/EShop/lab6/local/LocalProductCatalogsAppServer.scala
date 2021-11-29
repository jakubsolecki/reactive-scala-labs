package EShop.lab6.local
import EShop.lab5.ProductCatalog.GetItems
import EShop.lab5.{JsonSupport, ProductCatalog, SearchService}
import akka.actor.typed.scaladsl.Routers
import akka.http.scaladsl.server.Directives.{parameters, path}
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Try

class LocalProductCatalogsServer extends JsonSupport {
  implicit val system: ActorSystem[Nothing]               = ActorSystem(Behaviors.empty, "LocalReactiveRouters")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val timeout: Timeout                           = 5.seconds
  implicit val scheduler: Scheduler                       = system.scheduler

  val workers: ActorRef[ProductCatalog.Query] =
    system.systemActorOf(Routers.pool(5)(ProductCatalog(new SearchService())), "workersRouter")

  def routes: Route = {
    path("products") {
      get {
        parameters("brand".as[String], "words".as[String]) { (brand, words) =>
          complete {
            val items = workers
              .ask(ref => GetItems(brand, words.split(" ").toList, ref))
              .mapTo[ProductCatalog.Items]
            Future.successful(items)
          }
        }
      }
    }
  }

  def run(port: Int): Unit = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    Await.ready(bindingFuture, Duration.Inf)
  }
}

object LocalProductCatalogsServerApp extends App {
  val localProductCatalogsAppServer = new LocalProductCatalogsServer()
  localProductCatalogsAppServer.run(Try(args(0).toInt).getOrElse(9000))
}
