package controllers

import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import models._
import notifications.Notification.SendTo
import org.joda.time.DateTime
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc._
import sorm.Dsl._
import sorm.Persisted

/**
  * Created by thierry on 01.10.16.
  */
class MessageController @Inject()(@Named("notification") notification: ActorRef) extends Controller with Auth {

  def fetch(matchId: Long, fromDate: Long) = Authenticated { implicit request =>
    DB.query[Match].where(
      ("id" equal matchId) and
      (("user1" equal request.user) or ("user2" equal request.user))
    ).fetchOne map { discussion =>
      val messages =
        DB.query[Message]
          .whereEqual("discussion", discussion)
          .whereLarger("sentAt", new DateTime(fromDate))
          .order("sentAt")
          .fetch.map(Json.toJson(_)(Message.messagePersistedWrites))

      Ok(Json.obj("messages" -> messages))
    } getOrElse {
      Forbidden(Json.obj("error" -> "Access denied"))
    }
  }

  case class MessageContent(content: String, sentAt: DateTime)
  implicit val jsonMessageContent: Reads[MessageContent] = Json.reads[MessageContent]

  def store(matchId: Long) = Authenticated(parse.json) { implicit request =>
    request.body.validate[MessageContent].fold(
      errors => {
        BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      },
      message => {
        val user = request.user
        val discussion = DB.query[Match].where(
          ("id" equal matchId) and
          ("state" in Set(State.Pending, State.Open)) and
          (("user1" equal user) or ("user2" equal user))
        ).fetchOne()

        discussion map { discussion =>
          val storedMessage = DB save Message(discussion, Some(user), message.content, message.sentAt)
          val notif = Json.obj(
            "subject" -> "new-message",
            "matchId" -> discussion.id, "senderId" -> user.asInstanceOf[User with Persisted].id,
            "messageId" -> storedMessage.id, "content" -> storedMessage.content, "sentAt" -> storedMessage.sentAt
          )

          if (discussion.user1 == user)
            notification ! SendTo(discussion.user2, None, Some(notif))
          else
            notification ! SendTo(discussion.user1, None, Some(notif))

          Created(Json.toJson(storedMessage)(Message.messagePersistedWrites))
        } getOrElse {
          Forbidden(Json.obj("error" -> "Access denied"))
        }
      }
    )
  }
}
