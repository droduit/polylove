import models._
import org.scalatestplus.play._
import org.scalatest.Matchers._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Headers
import sorm.Persisted
import models._
import org.joda.time.DateTime

class MatchSpec extends PlaySpec with OneAppPerSuite {

  "MatchController#fetchMatches" should {

    "return the list of matches" in {
      DB.query[User].fetch.foreach(DB.delete(_))

      val userA = DB.save(User("userA@example.com", "User", "A", Set()))
      val userB = DB.save(User("userB@example.com", "User", "B", Set()))
      val userC = DB.save(User("userC@example.com", "User", "C", Set()))

      val matchAB = DB.save(Match(userA, userB, false, false, State.Close, DateTime.now.minusDays(1)))
      val matchAC = DB.save(Match(userA, userC, false, true, State.Pending, DateTime.now))
      val matchBC = DB.save(Match(userB, userC, true, true, State.Open, DateTime.now))

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, "/api/user/matches").withSession("userId" -> userA.id.toString))

      status(result) mustEqual OK

      val response = contentAsJson(result)

      val firstMatch = (response \ "matches")(0)
      val expectedFM =
        if ((firstMatch \ "match").as[Long] == matchAB.id) matchAB
        else if ((firstMatch \ "match").as[Long] == matchAC.id) matchAC
        else fail("Received an unexpected match")

      (firstMatch \ "user1").asOpt[Long] mustEqual Some(expectedFM.user1.asInstanceOf[User with Persisted].id)
      (firstMatch \ "user2").asOpt[Long] mustEqual Some(expectedFM.user2.asInstanceOf[User with Persisted].id)
      (firstMatch \ "state").asOpt[State.Value] mustEqual Some(expectedFM.state)
      (firstMatch \ "createdAt").asOpt[DateTime] mustEqual Some(expectedFM.createdAt)

      val secondMatch = (response \ "matches")(1)
      val expectedSM =
        if ((secondMatch \ "match").as[Long] == matchAB.id) matchAB
        else if ((secondMatch \ "match").as[Long] == matchAC.id) matchAC
        else fail("Received an unexpected match")

      (secondMatch \ "user1").asOpt[Long] mustEqual Some(expectedSM.user1.asInstanceOf[User with Persisted].id)
      (secondMatch \ "user2").asOpt[Long] mustEqual Some(expectedSM.user2.asInstanceOf[User with Persisted].id)
      (secondMatch \ "state").asOpt[State.Value] mustEqual Some(expectedSM.state)
      (secondMatch \ "createdAt").asOpt[DateTime] mustEqual Some(expectedSM.createdAt)
    }

  }

  "MatchController#validateMatch" should {

    def fixture = new {
      DB.query[User].fetch.foreach(DB.delete(_))

      val userA = DB save User("userA@example.com", "User", "A", Set())
      val userB = DB save User("userB@example.com", "User", "B", Set())
      val userC = DB save User("userC@example.com", "User", "C", Set())

      val profileB = DB save Profile(Some(userB), Section.IN, Gender.Female, DateTime.now, Set(), Set(), "", GenderInterest.Male, 20 to 25)
      val avatarB = DB save Avatar(Some(userB), HairColor.Blond, HairStyle.Style2, Eye.Blue, Skin.Medium, Shirt.Style1)

      val matchAB = DB save Match(userA, userB, false, false, State.Pending, DateTime.now)
      val matchAC = DB save Match(userA, userC, true, true, State.Open, DateTime.now)
      val matchBC = DB save Match(userB, userC, false, false, State.Pending, DateTime.now)
    }

    "accept valid request" in {
      val f = fixture

      val Some(resultA) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${f.matchAB.id}/interested",
          FakeHeaders(), Json.obj("interested" -> true)).withSession("userId" -> f.userA.id.toString))

      status(resultA) mustEqual OK

      val mA = DB.fetchById[Match](f.matchAB.id)

      mA.user1Interested mustEqual true
      mA.user2Interested mustEqual false

      val Some(resultB) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${f.matchAB.id}/interested",
          FakeHeaders(), Json.obj("interested" -> true)).withSession("userId" -> f.userB.id.toString))

      status(resultB) mustEqual OK

      val mB = DB.fetchById[Match](f.matchAB.id)

      mB.user1Interested mustEqual true
      mB.user2Interested mustEqual true
    }

    "reject invalid request" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${f.matchAB.id}/interested",
          FakeHeaders(), Json.obj("interested" -> "Not Bool")).withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual BAD_REQUEST

      val response = contentAsJson(result)

      ((response \ "errors" \ "obj.interested") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.expected.jsboolean"))
    }

    "reject request on non-pending matches" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${f.matchAC.id}/interested",
          FakeHeaders(), Json.obj("interested" -> false)).withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      (contentAsJson(result) \ "error").asOpt[String]
        .mustEqual(Some("No pending match found"))
    }

    "reject request on invalid matches" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/0/interested",
          FakeHeaders(), Json.obj("interested" -> true)).withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      (contentAsJson(result) \ "error").asOpt[String]
        .mustEqual(Some("No pending match found"))
    }

    "reject request on other matches" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${f.matchBC.id}/interested",
          FakeHeaders(), Json.obj("interested" -> true)).withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      (contentAsJson(result) \ "error").asOpt[String]
        .mustEqual(Some("No pending match found"))
    }

  }

}
