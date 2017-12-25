import org.scalatestplus.play._
import play.api.test._
import play.api.inject.guice.GuiceApplicationBuilder
import play.core.server.Server
import play.api.Play
import play.api.routing.sird._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import javax.inject._
import akka.actor._
import akka.testkit._
import org.scalatest._
import scala.language.postfixOps
import scala.concurrent.duration._

import play.api.libs.json._

import org.joda.time.LocalDate

import models._
import notifications._


class NotificationSpec() extends TestKit(ActorSystem("Notification"))
    with WordSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with OneInstancePerTest {

  val localFCMConfig =
    new GuiceApplicationBuilder()
      .configure(Map("fcm.baseUrl" -> ""))
      .build.configuration

  "notifications.Sender" should {

    "send valid notification and die" in {
      Server.withRouter() {
        case POST(p"/fcm/send") => Action {
          Results.Ok(Json.obj("multicast_id" -> 1234, "success" -> 1, "failure" -> 0, "canonical_ids" -> 0))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val idManagerProbe = TestProbe()

          val sender = system.actorOf(Props(new Sender(idManagerProbe.ref, localFCMConfig, client)))
          val senderProbe = TestProbe()

          senderProbe watch sender

          sender ! Sender.SendTo(
            "Toto", Some(JsString("Hello")), Some(JsString("World")))

          idManagerProbe expectNoMsg (500 millis)
          senderProbe expectTerminated sender
        }
      }
    }

    "send RenewId in case of registration_id" in {
      Server.withRouter() {
        case POST(p"/fcm/send") => Action {
          Results.Ok(Json.obj("multicast_id" -> 1234, "success" -> 1, "failure" -> 0, "canonical_ids" -> 1,
            "results" -> Json.arr(Json.obj("message_id" -> "5678", "registration_id" -> "Tata"))))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val idManagerProbe = TestProbe()

          val sender = system.actorOf(Props(new Sender(idManagerProbe.ref, localFCMConfig, client)))
          val senderProbe = TestProbe()

          senderProbe watch sender

          sender ! Sender.SendTo(
            "Toto", Some(JsString("Hello")), Some(JsString("World")))

          idManagerProbe expectMsg IdManager.RenewId("Toto", "Tata")
          senderProbe expectTerminated sender
        }
      }
    }

    "send RemoveId in case of NotRegistered" in {
      Server.withRouter() {
        case POST(p"/fcm/send") => Action {
          Results.Ok(Json.obj("multicast_id" -> 1234, "success" -> 1, "failure" -> 0, "canonical_ids" -> 1,
            "results" -> Json.arr(Json.obj("message_id" -> "5678", "error" -> "NotRegistered"))))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val idManagerProbe = TestProbe()

          val sender = system.actorOf(Props(new Sender(idManagerProbe.ref, localFCMConfig, client)))
          val senderProbe = TestProbe()

          senderProbe watch sender

          sender ! Sender.SendTo(
            "Toto", Some(JsString("Hello")), Some(JsString("World")))

          idManagerProbe expectMsg IdManager.RemoveId("Toto")
          senderProbe expectTerminated sender
        }
      }
    }

    "retry in case of bad status" in {
      Server.withRouter() {
        case POST(p"/fcm/send") => Action {
          Results.InternalServerError("Boom")
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val idManagerProbe = TestProbe()

          val senderProbe = TestProbe()
          val sender = system.actorOf(Props(new Sender(idManagerProbe.ref, localFCMConfig, client) {
            override val RetryAfterMax = 4
          }))

          senderProbe watch sender

          val notification = Sender.SendTo("Toto", Some(JsString("Hello")), Some(JsString("World")))
          sender ! notification

          EventFilter.error(pattern = "Bad status '500' returned").assertDone(1500 millis)
          EventFilter.debug(pattern = "Retrying to send after 1 seconds").assertDone(1500 millis)

          EventFilter.error(pattern = "Bad status '500' returned").assertDone(2500 millis)
          EventFilter.debug(pattern = "Retrying to send after 2 seconds").assertDone(2500 millis)

          EventFilter.error(pattern = "Bad status '500' returned").assertDone(4500 millis)
          EventFilter.debug(pattern = "Retrying to send after 4 seconds").assertDone(4500 millis)

          EventFilter.error(pattern = "Unable to send the following notification").assertDone(5000 millis)

          senderProbe expectTerminated (sender, 8 seconds)
        }
      }
    }

    "retry if Unavailable" in {
      Server.withRouter() {
        case POST(p"/fcm/send") => Action {
          Results.Ok(Json.obj("multicast_id" -> 1234, "success" -> 0, "failure" -> 1, "canonical_ids" -> 0,
            "results" -> Json.arr(Json.obj("error" -> "Unavailable"))))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val idManagerProbe = TestProbe()

          val senderProbe = TestProbe()
          val sender = system.actorOf(Props(new Sender(idManagerProbe.ref, localFCMConfig, client) {
            override val RetryAfterMax = 4
          }))

          senderProbe watch sender

          val notification = Sender.SendTo("Toto", Some(JsString("Hello")), Some(JsString("World")))
          sender ! notification

          EventFilter.error(pattern = "FCM is unavailable").assertDone(1500 millis)
          EventFilter.debug(pattern = "Retrying to send after 1 seconds").assertDone(1500 millis)

          EventFilter.error(pattern = "FCM is unavailable").assertDone(2500 millis)
          EventFilter.debug(pattern = "Retrying to send after 2 seconds").assertDone(2500 millis)

          EventFilter.error(pattern = "FCM is unavailable").assertDone(4500 millis)
          EventFilter.debug(pattern = "Retrying to send after 4 seconds").assertDone(4500 millis)

          EventFilter.error(pattern = "Unable to send the following notification").assertDone(5000 millis)

          senderProbe expectTerminated (sender, 8 seconds)
        }
      }
    }

    "retry if malformed response from FCM" in {
      Server.withRouter() {
        case POST(p"/fcm/send") => Action {
          Results.Ok(Json.obj("blablabla" -> "not valid"))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val idManagerProbe = TestProbe()

          val senderProbe = TestProbe()
          val sender = system.actorOf(Props(new Sender(idManagerProbe.ref, localFCMConfig, client) {
            override val RetryAfterMax = 4
          }))

          senderProbe watch sender

          val notification = Sender.SendTo("Toto", Some(JsString("Hello")), Some(JsString("World")))
          sender ! notification

          EventFilter.error(pattern = "FCM returned invalid response").assertDone(1500 millis)
          EventFilter.debug(pattern = "Retrying to send after 1 seconds").assertDone(1500 millis)

          EventFilter.error(pattern = "FCM returned invalid response").assertDone(2500 millis)
          EventFilter.debug(pattern = "Retrying to send after 2 seconds").assertDone(2500 millis)

          EventFilter.error(pattern = "FCM returned invalid response").assertDone(4500 millis)
          EventFilter.debug(pattern = "Retrying to send after 4 seconds").assertDone(4500 millis)

          EventFilter.error(pattern = "Unable to send the following notification").assertDone(5000 millis)

          senderProbe expectTerminated (sender, 8 seconds)
        }
      }
    }

  }

  "notifications.IdManager" should {

    before {
      DB.query[User].fetch.foreach(DB delete _)
      DB save User("just", "a", "test", Set("Toto", "Tata"))
    }

    "renew the firebase id when asked" in {
      val idManager = system.actorOf(IdManager.props)

      idManager ! IdManager.RenewId("Toto", "Titi")

      Thread sleep 1000

      val Some(user) = DB.query[User].fetchOne

      user.firebaseIds should equal (Set("Titi", "Tata"))
    }

    "remove the firebase id when asked" in {
      val idManager = system.actorOf(IdManager.props)

      idManager ! IdManager.RemoveId("Toto")

      Thread sleep 1000

      val Some(user) = DB.query[User].fetchOne

      user.firebaseIds should equal (Set("Tata"))
    }

  }

  "notification.Notification" should {

    "spawn a child and forget about the notification" in {
      Server.withRouter() {
        case POST(p"/fcm/send") => Action {
          Results.Ok(Json.obj("multicast_id" -> 1234, "success" -> 1, "failure" -> 0, "canonical_ids" -> 0))
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val senderProbe = TestProbe()

          val emptySenderFactory = new Sender.Factory {
            def apply(idManager: ActorRef) = new Actor() { def receive = { case _ => } }
          }

          val notification = system.actorOf(Props(new Notification(emptySenderFactory) {
            override def createSender = senderProbe.ref
          }))

          notification ! Notification.SendTo(
            User("Just", "a", "test", Set("Toto")), Some(JsString("Hello")), Some(JsString("World")))

          senderProbe expectMsg (500 millis,
            Sender.SendTo("Toto", Some(JsString("Hello")), Some(JsString("World"))))
        }
      }
    }

  }

}
