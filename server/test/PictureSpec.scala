import org.scalatestplus.play._
import org.scalatest.Matchers._
import play.api.test._
import play.api.test.Helpers._
import akka.actor._
import akka.stream._

import play.api.mvc._
import play.api.libs.json._

import models._
import sorm.Persisted
import org.joda.time.DateTime

class PictureSpec extends PlaySpec with OneAppPerSuite {

  def dummyUser: User with Persisted =
    DB.save(User("dummy@example.com", "Dummy", "User", Set())).asInstanceOf[User with Persisted]

  "PictureController#store" should {

    "store the picture on success" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/picture",
          FakeHeaders(Seq("Content-Type" -> "image/jpeg")), "Data of a picture...").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual CREATED
      contentType(result) shouldBe Some("application/json")
    }

    "store new version of picture" in {
      val u = dummyUser

      val Some(resultA) = route(app,
        FakeRequest(Helpers.POST, "/api/user/picture",
          FakeHeaders(Seq("Content-Type" -> "image/jpeg")), "Data of a picture...").withSession("userId" -> u.id.toString))

      status(resultA) mustEqual CREATED
      contentType(resultA) shouldBe Some("application/json")

      val Some(resultB) = route(app,
        FakeRequest(Helpers.POST, "/api/user/picture",
          FakeHeaders(Seq("Content-Type" -> "image/jpeg")), "Happy little clouds!").withSession("userId" -> u.id.toString))

      status(resultB) mustEqual CREATED
      contentType(resultB) shouldBe Some("application/json")
    }

    "reject non-JPEG request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/picture",
          FakeHeaders(Seq("Content-Type" -> "text/json")), "Data of a picture...").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST
      contentType(result) shouldBe Some("application/json")

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Picture must be a JPEG")
    }

  }

  "PictureController#fetchOwn" should {

    "return user's picture" in {
      val u = dummyUser

      val Some(create) = route(app, FakeRequest(Helpers.POST, "/api/user/picture",
        FakeHeaders(Seq("Content-Type" -> "image/jpeg")), "Data of a picture...").withSession("userId" -> u.id.toString))

      status(create) mustEqual CREATED

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, "/api/user/picture").withSession("userId" -> u.id.toString))

      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()

      status(result) mustEqual OK
      contentType(result) shouldBe Some("image/jpeg")
      contentAsString(result) mustEqual "Data of a picture..."
    }
  }

  "PictureController#fetch" should {

    def fixture = new {
      DB.query[User].fetch.foreach(DB.delete(_))

      val userA = DB save User("userA@example.com", "User", "A", Set())
      val userB = DB save User("userB@example.com", "User", "B", Set())
      val userC = DB save User("userC@example.com", "User", "C", Set())

      val matchAB = DB save Match(userA, userB, true, true, State.Open, DateTime.now)
      val matchBC = DB save Match(userB, userC, false, false, State.Close, DateTime.now)

      route(app,
        FakeRequest(Helpers.POST, "/api/user/picture",
          FakeHeaders(Seq("Content-Type" -> "image/jpeg")), "Data of a picture...").withSession("userId" -> userB.id.toString))
    }

    "return the picture on success" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userB.id}/picture")
          .withSession("userId" -> f.userA.id.toString))

      // Streamed response requires a materializer to be read
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()

      status(result) mustEqual OK
      contentType(result) mustEqual Some("image/jpeg")
      contentAsString(result) mustEqual "Data of a picture..."
    }

    "return NotFound if there is no picture" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userA.id}/picture")
          .withSession("userId" -> f.userB.id.toString))

      status(result) mustEqual NOT_FOUND

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("No picture for this user")
    }

    "reject if user doesn't exists" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/0/picture")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

    "reject if there is no match" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userC.id}/picture")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

  }

}
