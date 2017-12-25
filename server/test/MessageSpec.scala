import org.scalatestplus.play._
import org.scalatest.Matchers._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc.Headers
import sorm.Persisted
import models._
import org.joda.time.DateTime

class MessageSpec extends PlaySpec with OneAppPerSuite {

  "MessageController#storeMessage" should {

    def dummyUser(n: Int): User with Persisted =
      DB.save(User(s"dummy$n@example.com", s"Dummy$n", s"User$n", Set())).asInstanceOf[User with Persisted]

    def dummyMatch: (User with Persisted, User with Persisted, Match with Persisted) = {
      val (user1, user2) = (dummyUser(1), dummyUser(2))
      val discussion = DB save Match(user1, user2, true, true, State.Open, DateTime.now)

      (user1, user2, discussion)
    }

    "returns the stored message on success" in {
      val (user1, user2, discussion) = dummyMatch

      val now = DateTime.now
      val message = Json.obj("senderId" -> user1.id, "content" -> "Test", "sentAt" -> now)

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${discussion.id}/message",
          FakeHeaders(), Json.toJson(message)).withSession("userId" -> user1.id.toString))

      status(result) mustEqual CREATED
      contentType(result) mustEqual Some("application/json")

      val response = contentAsJson(result)

      (response \ "id").asOpt[Long] should not be empty
      (response \ "matchId").asOpt[Long] mustEqual Some(discussion.id)
      (response \ "senderId").asOpt[Long] mustEqual Some(user1.id)
      (response \ "content").asOpt[String] mustEqual Some("Test")
      (response \ "sentAt").asOpt[DateTime] mustEqual Some(now)
    }

    "save in DB a valid message" in {
      // Clear the database
      DB.query[Message].fetch.foreach(DB delete _)

      val (user1, user2, discussion) = dummyMatch

      val now = DateTime.now
      val message = Json.obj("senderId" -> user2.id, "content" -> "Test", "sentAt" -> now)

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${discussion.id}/message",
          FakeHeaders(), Json.toJson(message)).withSession("userId" -> user2.id.toString))

      status(result) mustEqual CREATED

      val Some(storedMessage) = DB.query[Message].fetchOne

      storedMessage.discussion.asInstanceOf[Match with Persisted].id mustEqual discussion.id
      storedMessage.sender.get.asInstanceOf[User with Persisted].id mustEqual user2.id
      storedMessage.content mustEqual "Test"
      storedMessage.sentAt mustEqual now
    }

    "rejects a non-JSON request" in {
      val (user1, user2, discussion) = dummyMatch

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${discussion.id}/message",
          FakeHeaders(Seq(CONTENT_TYPE -> "text/plain")), "Hello API").withSession("userId" -> user1.id.toString))

      status(result) mustEqual UNSUPPORTED_MEDIA_TYPE

      (contentAsJson(result) \ "error").asOpt[String]
        .mustEqual(Some("Expecting text/json or application/json body"))
    }

    "rejects an incomplete message" in {
      val (user1, user2, discussion) = dummyMatch

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${discussion.id}/message",
          FakeHeaders(), Json.obj("senderId" -> "abc", "content" -> 1234)).withSession("userId" -> user1.id.toString))

      status(result) mustEqual BAD_REQUEST

      val response = contentAsJson(result)

      ((response \ "errors" \ "obj.sentAt") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.path.missing"))
      ((response \ "errors" \ "obj.content") (0) \ "msg") (0).asOpt[String]
        .mustEqual(Some("error.expected.jsstring"))
    }

    "rejects message to an invalid match" in {
      val (user1, user2, discussionA) = dummyMatch
      val (user3, user4, discussionB) = dummyMatch

      val now = DateTime.now
      val message = Json.obj("senderId" -> user1.id, "content" -> "Test", "sentAt" -> now)

      val Some(result) = route(app,
        FakeRequest(Helpers.POST, s"/api/match/${discussionB.id}/message",
          FakeHeaders(), Json.toJson(message)).withSession("userId" -> user1.id.toString))

      status(result) mustEqual FORBIDDEN

      val response = contentAsJson(result)

      (response \ "error").asOpt[String] mustEqual Some("Access denied")
    }
  }

}
