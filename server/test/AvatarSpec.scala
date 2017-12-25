import org.scalatestplus.play._
import org.scalatest.Matchers._
import play.api.test._
import play.api.test.Helpers._

import play.api.mvc._
import play.api.libs.json._

import models._
import sorm.Persisted
import org.joda.time.DateTime

class AvatarSpec extends PlaySpec with OneAppPerSuite {

  def dummyUser: User with Persisted =
    DB.save(User("dummy@example.com", "Dummy", "User", Set())).asInstanceOf[User with Persisted]

  "AvatarController#store" should {

    val validAvatar = Avatar(None, HairColor.Blond, HairStyle.Style2, Eye.Blue, Skin.Medium, Shirt.Style1)
    val incompleteAvatar = Json.obj("hairColor" -> "Pink", "eyes" -> "Blue")

    "returns the stored avatar on success" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(), Json.toJson(validAvatar)).withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual CREATED
      contentType(result) mustEqual Some("application/json")

      val response = contentAsJson(result)

      (response \ "id").asOpt[Long] should not be empty
    }

    "save in DB a valid avatar" in {
      // Clear the database
      DB.query[Avatar].fetch.foreach(DB delete _)
      val du = dummyUser

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(), Json.toJson(validAvatar)).withSession("userId" -> du.id.toString))

      status(result) mustEqual CREATED

      val Some(avatar) = DB.query[Avatar].fetchOne

      // Drop the Persisted trait
      avatar.mixoutPersisted._2 mustEqual (validAvatar copy (user = Some(du)))
    }

    "update avatar and not insert a new one" in {
      DB.query[User].fetch.foreach(DB delete _)
      val du = dummyUser

      val Some(createResult) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(), Json.toJson(validAvatar)).withSession("userId" -> du.id.toString))

      status(createResult) mustEqual CREATED

      val Some(updateResult) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(), Json.toJson(validAvatar copy (skin = Skin.Light))).withSession("userId" -> du.id.toString))

      status(updateResult) mustEqual OK

      DB.query[Avatar].count mustEqual 1
    }

    "rejects a non-JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(Seq(CONTENT_TYPE -> "text/plain")), "Hello API").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual UNSUPPORTED_MEDIA_TYPE

      (contentAsJson(result) \ "error").asOpt[String]
        .mustEqual(Some("Expecting text/json or application/json body"))
    }

    "rejects a disguised as JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(Seq(CONTENT_TYPE -> "application/json")), "Hello again API").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      (contentAsJson(result) \ "error").asOpt[String].getOrElse("")
      startWith("Invalid Json")
    }

    "rejects an empty JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(Seq(CONTENT_TYPE -> "application/json")), "").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      (contentAsJson(result) \ "error").asOpt[String].getOrElse("")
      startWith("Invalid Json")
    }

    "rejects an incomplete avatar" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/avatar",
          FakeHeaders(), incompleteAvatar).withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      val response = contentAsJson(result)

      ((response \ "errors" \ "obj.skin") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
      ((response \ "errors" \ "obj.hairStyle") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
      ((response \ "errors" \ "obj.hairColor") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.value.invalid"))
    }

  }

  "AvatarController#fetch" should {

    def fixture = new {
      DB.query[User].fetch.foreach(DB.delete(_))

      val userA = DB save User("userA@example.com", "User", "A", Set())
      val userB = DB save User("userB@example.com", "User", "B", Set())
      val userC = DB save User("userC@example.com", "User", "C", Set())

      val profileB = DB save Profile(Some(userB), Section.IN, Gender.Female, DateTime.now, Set(), Set(), "", GenderInterest.Male, 20 to 25)
      val avatarB = DB save Avatar(Some(userB), HairColor.Blond, HairStyle.Style2, Eye.Blue, Skin.Medium, Shirt.Style1)

      val matchAB = DB save Match(userA, userB, true, true, State.Open, DateTime.now)
      val matchBC = DB save Match(userB, userC, false, false, State.Close, DateTime.now)
    }

    "return the avatar on a valid request" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userB.id}/avatar")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual OK

      val response = contentAsJson(result)

      (response \ "gender").asOpt[String] mustEqual Some("Female")
      (response \ "avatar" \ "id").asOpt[Long] should not be empty
      (response \ "avatar" \ "hairColor").asOpt[String] mustEqual Some("Blond")
      (response \ "avatar" \ "hairStyle").asOpt[String] mustEqual Some("Style2")
      (response \ "avatar" \ "eyes").asOpt[String] mustEqual Some("Blue")
      (response \ "avatar" \ "skin").asOpt[String] mustEqual Some("Medium")
    }

    "return null if there is no avatar" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userA.id}/avatar")
          .withSession("userId" -> f.userB.id.toString))

      status(result) mustEqual OK

      val response = contentAsJson(result)

      (response \ "gender") mustEqual JsDefined(JsNull)
      (response \ "avatar") mustEqual JsDefined(JsNull)
    }

    "reject if user doesn't exists" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/0/avatar")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

    "reject if there is no match" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userC.id}/avatar")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

  }

}
