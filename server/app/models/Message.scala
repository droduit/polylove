package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime
import sorm.Persisted

/**
  * Created by thierry on 01.10.16.
  */

case class Message
  (discussion: Match, sender: Option[User], content: String, sentAt: DateTime)


object Message {
  implicit val messageFormat = Json.format[Message]
  implicit val messagePersistedWrites: Writes[Message with Persisted] = (
    (JsPath \ "id").write[Long] and
    (JsPath \ "matchId").write[Long] and
    (JsPath \ "senderId").write[Long] and
    (JsPath \ "content").write[String] and
    (JsPath \ "sentAt").write[DateTime]
  )(unlift(Message.unapplyPersisted))

  def unapplyPersisted(m: Message with Persisted) = {
    Some((
      m.id, m.discussion.asInstanceOf[Match with Persisted].id,
      m.sender.map(_.asInstanceOf[User with Persisted].id).getOrElse(0l),
      m.content, m.sentAt
    ))
  }
}

