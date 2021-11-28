package EShop.lab6.cluster

import EShop.lab5.ProductCatalog.GetItems
import EShop.lab5.{JsonSupport, ProductCatalog, SearchService}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Try

object SeedNode extends App {
  private val instancesPerNode = 2
  private val config           = ConfigFactory.load()
  val system = ActorSystem[Nothing](
    Behaviors.empty,
    "ProductCatalog",
    config.getConfig(Try(args(0)).getOrElse("seed-node1")).withFallback(config)
  )

  for (i <- 1 to instancesPerNode)
    system.systemActorOf(ProductCatalog(new SearchService()), s"worker-$i")

  Await.ready(system.whenTerminated, Duration.Inf)
}

class ClusterProductCatalogServer() extends JsonSupport {
  private val config = ConfigFactory.load()

  implicit val system = ActorSystem[Nothing](
    Behaviors.empty,
    "ProductCatalog",
    config
  )

  implicit val scheduler        = system.scheduler
  implicit val executionContext = system.executionContext
  implicit val timeout: Timeout = 5.seconds

  val requestsCounter: ActorRef[RequestCounter.Command] =
    system.systemActorOf(Routers.group(RequestCounterActor.RequestCounterServiceKey), "requestCounterRouter")

  val workers: ActorRef[ProductCatalog.Query] =
    system.systemActorOf(Routers.group(ProductCatalog.ProductCatalogServiceKey), "clusterWorkerRouter")

  def routes: Route = concat(
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
    },
    path("counter") {
      get {
        complete {
          val count = requestsCounter
            .ask(ref => RequestCounter.ProductRequestsCount(ref))
            .map { count => s"{ \"count\": $count }" }
          Future.successful(count)
        }
      }
    }
  )

  def run(port: Int): Unit = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    Await.ready(bindingFuture, Duration.Inf)
  }
}

object ClusterProductCatalogServerApp extends App {
  val clusterProductCatalogServer = new ClusterProductCatalogServer()
  clusterProductCatalogServer.run(Try(args(0).toInt).getOrElse(9000))
}
