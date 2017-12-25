import org.scalatestplus.play._
import org.scalatest.Matchers._
import play.api.test._
import play.api.test.Helpers._

import play.api.mvc._
import play.api.libs.json._

import models._
import sorm.Persisted
import org.joda.time.DateTime

class ProfileSpec extends PlaySpec with OneAppPerSuite {

  def dummyUser: User with Persisted =
    DB.save(User("dummy@example.com", "Dummy", "User", Set())).asInstanceOf[User with Persisted]

  "ProfileController#store" should {

    val validProfile = Profile(None, Section.CGC, Gender.Other, new DateTime(2016, 3, 1, 0, 0), Set(Language.French, Language.English), Set("Lot's", "of", "stuff"), "Blah blah blah", GenderInterest.Both, 18 to 20)
    val incompleteProfile = Json.obj(
      "gender" -> Gender.Male, "description" -> "Undisclosed",
      "genderInterest" -> Gender.Female, "ageInterest" -> Json.obj("start" -> 42)
    )

    "returns the stored profile on success" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(), Json.toJson(validProfile)).withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual CREATED
      contentType(result) mustEqual Some("application/json")

      val response = contentAsJson(result)

      (response \ "id").asOpt[Long] should not be empty
    }

    "save in DB a valid profile" in {
      // Clear the database
      DB.query[Profile].fetch.foreach(DB delete _)
      val du = dummyUser

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(), Json.toJson(validProfile)).withSession("userId" -> du.id.toString))

      status(result) mustEqual CREATED

      val Some(profile) = DB.query[Profile].fetchOne

      // Drop the Persisted trait
      profile.mixoutPersisted._2 mustEqual (validProfile copy (user = Some(du)))
    }

    "update profile and not insert a new one" in {
      DB.query[User].fetch.foreach(DB delete _)
      val du = dummyUser

      val Some(createResult) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(), Json.toJson(validProfile)).withSession("userId" -> du.id.toString))

      status(createResult) mustEqual CREATED

      val Some(updateResult) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(), Json.toJson(validProfile copy (gender = Gender.Male))).withSession("userId" -> du.id.toString))

      status(updateResult) mustEqual OK

      DB.query[Profile].count mustEqual 1
    }

    "rejects a non-JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(Seq(CONTENT_TYPE -> "text/plain")), "Hello API").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual UNSUPPORTED_MEDIA_TYPE

      (contentAsJson(result) \ "error").asOpt[String]
        .mustEqual(Some("Expecting text/json or application/json body"))
    }

    "rejects a disguised as JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(Seq(CONTENT_TYPE -> "application/json")), "Hello again API").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      (contentAsJson(result) \ "error").asOpt[String].getOrElse("")
      startWith("Invalid Json")
    }

    "rejects an empty JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(Seq(CONTENT_TYPE -> "application/json")), "").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      (contentAsJson(result) \ "error").asOpt[String].getOrElse("")
      startWith("Invalid Json")
    }

    "rejects an incomplete profile" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/profile",
          FakeHeaders(), incompleteProfile).withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      val response = contentAsJson(result)

      ((response \ "errors" \ "obj.birthday") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
      ((response \ "errors" \ "obj.hobbies") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
      ((response \ "errors" \ "obj.ageInterest") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
      ((response \ "errors" \ "obj.languages") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
    }

  }

  "ProfileController#fetch" should {

    def fixture = new {
      DB.query[User].fetch.foreach(DB.delete(_))

      val userA = DB save User("userA@example.com", "User", "A", Set())
      val userB = DB save User("userB@example.com", "User", "B", Set())
      val userC = DB save User("userC@example.com", "User", "C", Set())

      val profileB = DB save Profile(Some(userB), Section.IN, Gender.Male, DateTime.now, Set(Language.French), Set("Fun"), "Description", GenderInterest.Both, (20 to 25))

      val matchAB = DB save Match(userA, userB, true, true, State.Open, DateTime.now)
      val matchBC = DB save Match(userB, userC, false, false, State.Close, DateTime.now)
    }

    "return the profile on a valid request" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userB.id}/profile")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual OK

      val response = contentAsJson(result)

      (response \ "id").asOpt[Long] should not be empty
      (response \ "section").asOpt[String] mustEqual Some("IN")
      (response \ "languages").asOpt[Set[String]] mustEqual Some(Set("French"))
      (response \ "genderInterest").asOpt[String] mustEqual Some("Both")
      (response \ "ageInterest" \ "start").asOpt[Int] mustEqual Some(20)
      (response \ "ageInterest" \ "end").asOpt[Int] mustEqual Some(25)
    }

    "return null if there is no profile" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userA.id}/profile")
          .withSession("userId" -> f.userB.id.toString))

      status(result) mustEqual OK

      contentAsJson(result) mustEqual JsNull
    }

    "reject if user doesn't exists" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/0/profile")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

    "reject if there is no match" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userC.id}/profile")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

  }

}
