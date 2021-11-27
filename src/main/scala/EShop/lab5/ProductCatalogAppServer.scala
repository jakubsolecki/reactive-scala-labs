package EShop.lab5

import java.net.URI
import EShop.lab5.ProductCatalog.{GetItems, Item, Items}
import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat}

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.{Duration, DurationInt}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI =
      json match {
        case JsString(url) => new URI(url)
        case _             => throw new RuntimeException("Parsing exception")
      }
  }

  implicit val itemFormat: RootJsonFormat[Item]    = jsonFormat5(Item)
  implicit val returnFormat: RootJsonFormat[Items] = jsonFormat1(Items)
}

object ProductCatalogHttpServerApp extends App {
  ProductCatalogHttpServer.start(9000)
}

case class ProductCatalogHttpServer(queryRef: ActorRef[ProductCatalog.Query])(implicit val scheduler: Scheduler)
  extends JsonSupport {
  implicit val timeout: Timeout = 3.second

  def routes: Route = {
    path("catalog") {
      get {
        parameters("brand".as[String], "words".as[String]) { (brand, words) =>
          complete {
            val items = queryRef
              .ask(ref => GetItems(brand, words.split(" ").toList, ref))
              .mapTo[ProductCatalog.Items]
            Future.successful(items)
          }
        }
      }
    }
  }
}

object ProductCatalogHttpServer {
  def apply(port: Int): Behavior[Receptionist.Listing] = {
    Behaviors.setup { context =>
      implicit val executionContext: ExecutionContextExecutor = context.executionContext
      implicit val system: ActorSystem[Nothing]               = context.system
      implicit val timeout: Timeout                           = 3.second
      implicit val scheduler: Scheduler                       = system.scheduler

      system.receptionist ! Receptionist.subscribe(ProductCatalog.ProductCatalogServiceKey, context.self)
      Behaviors.receiveMessage[Receptionist.Listing] { msg =>
        val listing = msg.serviceInstances(ProductCatalog.ProductCatalogServiceKey)
        if (listing.isEmpty)
          Behaviors.same
        else {
          val queryRef = listing.head
          val selfRef  = ProductCatalogHttpServer(queryRef)
          val binding  = Http().newServerAt("localhost", port).bind(selfRef.routes)
          val _        = Await.ready(binding, Duration.Inf)
          Behaviors.empty
        }
      }
    }
  }

  def start(port: Int): Future[Done] = {
    val system = ActorSystem[Receptionist.Listing](ProductCatalogHttpServer(port), "ProductCatalog")
    Await.ready(system.whenTerminated, Duration.Inf)
  }
}
