package models

import play.api.libs.json.{JsPath, Json, Writes}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import sorm.Dsl._
import sorm.Persisted

/**
  * Created by Dominique on 01.10.16.
  */
object State extends Enumeration {
  /*
  * Pending :  match proposal is sent and the confirmation is pending
  * Close : the match was not confirmed by both users
  * Open : the match stay alive, both users confirmed it
  */
  val Pending, Close, Open = Value

  implicit val stateReads = new EnumReads[State.type](State)
}

case class Match
  (user1: User, user2: User, user1Interested: Boolean, user2Interested: Boolean,
   state: State.Value, createdAt: DateTime)

object Match {
  implicit val matchFormat = Json.format[Match]
  implicit val matchPersistedWrites: Writes[Match with Persisted] = (
    (JsPath \ "match").write[Long] and
    (JsPath \ "user1").write[Long] and
    (JsPath \ "user2").write[Long] and
    (JsPath \ "state").write[String] and
    (JsPath \ "createdAt").write[DateTime]
    )(unlift(Match.unapplyPersisted))

  def unapplyPersisted(m: Match with Persisted) = {
    Some((
      m.id, m.user1.asInstanceOf[User with Persisted].id, m.user2.asInstanceOf[User with Persisted].id,
      m.state.toString, m.createdAt
      ))
  }

  def between(userA: User, userB: User, states: Set[State.Value]): Option[Match] =
    DB.query[Match].where(
      ("state" in states) and
      ((("user1" equal userA) and ("user2" equal userB)) or
       (("user1" equal userB) and ("user2" equal userA)))
    ).fetchOne
}