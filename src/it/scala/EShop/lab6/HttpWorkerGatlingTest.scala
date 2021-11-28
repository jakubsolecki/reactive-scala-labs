package EShop.lab6

import io.gatling.core.Predef.{Simulation, StringBody, jsonFile, rampUsers, scenario, _}
import io.gatling.http.Predef.http

import scala.concurrent.duration._

class HttpWorkerGatlingTest extends Simulation {

  val httpProtocol = http  //values here are adjusted to cluster_demo.sh script
    .baseUrls("http://localhost:9001", "http://localhost:9002")
    .acceptHeader("text/plain,text/html,application/json,application/xml;")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val scn = scenario("BasicSimulation")
//    .feed(jsonFile(classOf[HttpWorkerGatlingTest].getResource("/data/work_data.json").getPath).random)
    .feed(csv(fileName="data/product_data.csv", quoteChar=',').random)
    .exec(
      http("request")
        .get("/products?brand=${brand}&words=${word}")
        .asJson
    )
    .pause(5)

  setUp(
    scn.inject(
      incrementUsersPerSec(40)
        .times(10)
        .eachLevelLasting(20.seconds)
        .separatedByRampsLasting(1.seconds)
        .startingFrom(1000)
    )
  ).protocols(httpProtocol)
}