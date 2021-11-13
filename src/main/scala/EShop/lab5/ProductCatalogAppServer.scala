package EShop.lab5

import java.net.URI
import EShop.lab5.ProductCatalog.{GetItems, Item, Items}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.util.Timeout
import scalaz.Scalaz.ToFunctorOpsUnapply
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

object ProductAppHttpServer extends App {
  new AkkaHttpServer().start(9000)
}

class AkkaHttpServer extends JsonSupport {
  implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "ProductCatalog")
  implicit val executionContext             = system.executionContext
  implicit val timeout: Timeout             = 3.second
  implicit val scheduler                    = system.scheduler

  def routes: Route = {
    path("catalog") {
      get {
        parameters("brand".as[String], "words".as[String]) { (brand, words) =>
          val listingFuture = system.receptionist.ask((ref: ActorRef[Receptionist.Listing]) =>
            Receptionist.find(ProductCatalog.ProductCatalogServiceKey, ref)
          )
          val items = for {
            ProductCatalog.ProductCatalogServiceKey.Listing(listing) <- listingFuture
            productCatalog = listing.head
            items <-
              productCatalog.ask(ref => GetItems(brand, words.split(" ").toList, ref)).mapTo[ProductCatalog.Items]
          } yield items
          complete(items)
        }
      }
    }
  }

  def start(port: Int) = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    Await.ready(system.whenTerminated, Duration.Inf)
  }
}
