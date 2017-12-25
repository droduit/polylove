package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import sorm.Persisted

/**
  * Created by Dominique on 01.10.16.
  */
case class User (email: String, firstname: String, lastname: String, firebaseIds: Set[String]) {
  def profile: Option[Profile with Persisted] =
    DB.query[Profile].whereEqual("user", Some(this)).order("id", true).fetchOne

  def avatar: Option[Avatar with Persisted] =
    DB.query[Avatar].whereEqual("user", Some(this)).order("id", true).fetchOne
}

object User {
  implicit val userFormat = Json.format[User]
  implicit val userPersistedWrites: Writes[User with Persisted] = (
    (JsPath \ "id").write[Long] and
    (JsPath \ "email").write[String] and
    (JsPath \ "firstname").write[String] and
    (JsPath \ "lastname").write[String]
  )(unlift(User.unapplyPersisted))

  def unapplyPersisted(u: User with Persisted) = {
    Some((u.id, u.email, u.firstname, u.lastname))
  }
}
