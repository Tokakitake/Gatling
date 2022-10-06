package simulations

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

class TestTask extends Simulation {

  val httpConf = http.baseUrl("http://localhost:8080/app/")
    .header("content-type", "application/json")
    //для фиддлера
    //.proxy(Proxy("localhost", 8888))

  var idNumbers = Iterator.from(11)
  val rnd = new Random()
  val now = LocalDate.now()
  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }

  val customFeeder = Iterator.continually(Map(
    "gameId" -> idNumbers.next(),
    "name" -> ("Game-" + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category-" + randomString(6)),
    "rating" -> ("Rating-" + randomString(4))
  ))

  val customFeederUpdate = Iterator.continually(Map(
    "name" -> ("Game-" + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category-" + randomString(6)),
    "rating" -> ("Rating-" + randomString(4))
  ))

  def postNewGame() = {
    feed(customFeeder)
      .exec(http("Create New Game")
        .post("videogames/")
        .body(ElFileBody("data/games.json")).asJson
        .check(status.is(200)))
  }

  def getAllGames() = {
    exec(http("Get All Games")
      .get("videogames")
      .check(status.is(200)))
  }

  def updateLastGame() = {
    feed(customFeederUpdate)
      .exec(http("Update last game")
        .put("videogames/${gameId}")
        .body(ElFileBody("data/games.json")).asJson
        .check(jsonPath("$.name").is("${name}"))
        .check(status.is(200)))
  }

  val scn = scenario("Load Test")
    .forever() {
      exec(postNewGame())
        .exec(getAllGames())
        .exec(updateLastGame())
        .pause(10)
    }

  setUp(
    scn.inject(
      atOnceUsers(100)
    ).protocols(httpConf)
  ).maxDuration(1800)

}
