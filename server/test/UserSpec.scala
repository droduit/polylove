import org.scalatestplus.play._
import org.scalatest.Matchers._
import play.api.test._
import play.api.test.Helpers._

import play.api.mvc._
import play.api.libs.json._

import play.core.server.Server
import play.api.routing._
import play.api.routing.sird._
import play.api.inject.guice.GuiceApplicationBuilder

import models._
import controllers.UserController
import controllers.AvatarController
import tequila.Tequila
import sorm.Persisted
import org.joda.time.DateTime

class UserSpec extends PlaySpec with OneAppPerSuite {
  val token = "eP_hKd9mrU8:APA91bE76pzXMGw8oWe1aKYYLu57S2kE97CW-KibSlMsm-kULRq3gFPiQDXWYzgv_djkKiIo_H9TCsN7iOpJIkTivbhHBiq6QLPWhKYob6sMotKB4psIYKBX7gGKLYw86Pt3shycQIq3";

  def dummyUser: User with Persisted =
    DB.save(User("dummy@example.com", "Dummy", "User", Set())).asInstanceOf[User with Persisted]

  "UserController#storeFirebaseId" should {

    "store a valid firebaseId with no error" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/firebaseId", FakeHeaders(),
          Json.obj("id" -> token)).withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual CREATED
      contentType(result) mustEqual Some("application/json")
    }

    "save in DB a valid firebaseId" in {
      // Clear the database
      DB.query[User].fetch.foreach(DB delete _)
      val du = dummyUser

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/firebaseId", FakeHeaders(),
          Json.obj("id" -> token)).withSession("userId" -> du.id.toString))

      status(result) mustEqual CREATED

      val user = DB.fetchById[User](du.id)

      user.firebaseIds must contain(token)
    }

    "reject an empty JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/firebaseId",
          FakeHeaders(Seq(CONTENT_TYPE -> "application/json")), "").withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      (contentAsJson(result) \ "error").asOpt[String].getOrElse("")
      startWith("Invalid Json")
    }

    "reject an incomplete JSON request" in {
      val Some(result) = route(app,
        FakeRequest(Helpers.POST, "/api/user/firebaseId",
          FakeHeaders(), Json.obj()).withSession("userId" -> dummyUser.id.toString))

      status(result) mustEqual BAD_REQUEST

      val response = contentAsJson(result)

      ((response \ "errors" \ "obj.id") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
    }

  }

  "UserController#fetch" should {

    def fixture = new {
      DB.query[User].fetch.foreach(DB.delete(_))

      val userA = DB save User("userA@example.com", "User", "A", Set())
      val userB = DB save User("userB@example.com", "User", "B", Set())
      val userC = DB save User("userC@example.com", "User", "C", Set())

      val avatarB  = DB save Avatar(Some(userB), HairColor.Blond, HairStyle.Style2, Eye.Blue, Skin.Medium, Shirt.Style1)
      val profileB = DB save Profile(Some(userB), Section.IN, Gender.Male, DateTime.now, Set(Language.French), Set("Fun"), "Description", GenderInterest.Both, (20 to 25))

      val matchAB = DB save Match(userA, userB, true, true, State.Open, DateTime.now)
      val matchBC = DB save Match(userB, userC, false, false, State.Close, DateTime.now)
    }

    "return the user with its profile and avatar on a valid request" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userB.id}")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual OK

      val response = contentAsJson(result)

      (response \ "avatar" \ "id").asOpt[Long] should not be empty
      (response \ "profile" \ "id").asOpt[Long] should not be empty
      (response \ "user" \ "id").asOpt[Long] should not be empty
      (response \ "user" \ "email").asOpt[String] mustEqual Some("userB@example.com")
      (response \ "user" \ "firstname").asOpt[String] mustEqual Some("User")
      (response \ "user" \ "lastname").asOpt[String] mustEqual Some("B")
    }

    "return null for profile and avatar if there is none" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userA.id}")
          .withSession("userId" -> f.userB.id.toString))

      status(result) mustEqual OK

      val response = contentAsJson(result)

      (response \ "user" \ "id").asOpt[Int] should not be empty
      (response \ "avatar") mustEqual JsDefined(JsNull)
      (response \ "profile") mustEqual JsDefined(JsNull)
    }

    "reject if user doesn't exists" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/0")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

    "reject if there is no match" in {
      val f = fixture

      val Some(result) = route(app,
        FakeRequest(Helpers.GET, s"/api/user/${f.userC.id}")
          .withSession("userId" -> f.userA.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }

  }

  val localTequilaConfig =
    new GuiceApplicationBuilder()
      .configure(Map("tequila.baseUrl" -> ""))
      .build.configuration

  "UserController#login" should {

    "create user and log in" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("access_token" -> "1234"))
        }
        case sird.GET(p"/cgi-bin/OAuth2IdP/userinfo") => Action {
          Results.Ok(Json.obj("Sciper" -> 123456, "Username" -> "toto", "Email" -> "toto2@example.com", "Firstname" -> "toto", "Name" -> "Mr."))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)
          val ac = new AvatarController()

          val signup = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(signup) mustEqual CREATED
          session(signup).get("userId") should not be empty

          val signupResponse = contentAsJson(signup)

          (signupResponse \ "created").asOpt[Boolean] shouldBe Some(true)
          (signupResponse \ "user").asOpt[User] shouldBe None

          val Some(userId) = session(signup).get("userId")
          val avatar = ac.store().apply(
            FakeRequest(Helpers.POST, "/api/user/avatar",
              FakeHeaders(), Json.toJson(Avatar(None, HairColor.Blond, HairStyle.Style1, Eye.Blue,
                Skin.Medium, Shirt.Style1))).withSession("userId" -> userId.toString))

          status(avatar) mustEqual CREATED

          val login = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(login) mustEqual OK
          session(login).get("userId") should not be empty

          val loginResponse = contentAsJson(login)

          (loginResponse \ "created").asOpt[Boolean] shouldBe Some(false)
          (loginResponse \ "user" \ "id").asOpt[Long] should not be empty
          (loginResponse \ "avatar" \ "id").asOpt[Long] should not be empty
        }
      }
    }

    "reject login if fetchToken failed" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.InternalServerError("Internal Server Error")
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Tequila request failed"))
        }
      }
    }

    "reject login if fetchProfile failed" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("access_token" -> "1234"))
        }
        case sird.GET(p"/cgi-bin/OAuth2IdP/userinfo") => Action {
          Results.InternalServerError("Internal Server Error")
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Tequila request failed"))
        }
      }
    }

    "reject login if fetchToken returned a non-JSON response" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("not" -> "known"))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Tequila error: Unknown response"))
        }
      }
    }

    "reject login if fetchProfile returned a non-JSON response" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("access_token" -> "1234"))
        }
        case sird.GET(p"/cgi-bin/OAuth2IdP/userinfo") => Action {
          Results.Ok(Json.obj("not" -> "known"))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Tequila error: Unknown response"))
        }
      }
    }

    "reject login if fetchToken returned invalid response" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok("This is supposed to be some JSON")
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Internal error"))
        }
      }
    }

    "reject login if fetchProfile returned invalid response" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("access_token" -> "1234"))
        }
        case sird.GET(p"/cgi-bin/OAuth2IdP/userinfo") => Action {
          Results.Ok("This is supposed to be some JSON")
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Internal error"))
        }
      }
    }

    "reject login if fetchToken returned an error" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("error" -> "Auth failed"))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Tequila error: Auth failed"))
        }
      }
    }

    "reject login if fetchProfile returned an error" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("access_token" -> "1234"))
        }
        case sird.GET(p"/cgi-bin/OAuth2IdP/userinfo") => Action {
          Results.Ok(Json.obj("error" -> "Auth failed"))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("id" -> "toto")))

          status(result) mustEqual INTERNAL_SERVER_ERROR
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          (response \ "error").asOpt[String]
            .mustEqual(Some("Tequila error: Auth failed"))
        }
      }
    }

    "reject login LoginRequest is invalid" in {
      Server.withRouter() {
        case sird.GET(p"/cgi-bin/OAuth2IdP/token") => Action {
          Results.Ok(Json.obj("access_token" -> "1234"))
        }
        case sird.GET(p"/cgi-bin/OAuth2IdP/userinfo") => Action {
          Results.Ok(Json.obj("Sciper" -> 123456, "Gaspar" -> "toto", "Email" -> "toto2@example.com", "Firstname" -> "toto", "Lastname" -> "Mr."))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tequila = new Tequila(localTequilaConfig, client)
          val uc = new UserController(tequila)

          val result = uc.login().apply(
            FakeRequest(Helpers.POST, "/api/login", FakeHeaders(),
              Json.obj("invalid" -> "object")))

          status(result) mustEqual BAD_REQUEST
          session(result).get("userId") shouldBe empty

          val response = contentAsJson(result)

          ((response \ "errors" \ "obj.id") (0) \ "msg") (0).asOpt[String]
            .mustEqual(Some("error.path.missing"))
        }
      }
    }

  }

}
