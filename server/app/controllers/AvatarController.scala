package controllers

import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import sorm.Persisted

import models._

/**
  * Created by thierry on 08.10.16.
  */
class AvatarController extends Controller with Auth {

  def store = Authenticated(parse.json) { implicit request =>
    request.body.validate[Avatar].fold(
      errors => {
        BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      },
      avatar => {
        request.user.avatar match {
          case None =>
            val storedAvatar = DB save (avatar copy (user = Some(request.user)))

            Created(Json.toJson(storedAvatar)(Avatar.avatarPersistedWrites))

          case Some(oldAvatar) =>
            val storedAvatar = DB save (oldAvatar copy (
              hairColor = avatar.hairColor,
              hairStyle = avatar.hairStyle,
              eyes = avatar.eyes,
              skin = avatar.skin
            ))

            Ok(Json.toJson(storedAvatar)(Avatar.avatarPersistedWrites))
        }
      }
    )
  }

  def fetch(targetId: Long) = Authenticated { implicit request =>
    DB.query[User].whereEqual("id", targetId).fetchOne map { targetUser =>
      Match between (request.user, targetUser, State.values.toSet) match {
        case None => Forbidden(Json.obj("error" -> "Access denied"))
        case Some(_) =>
          val gender = targetUser.profile map (_.gender)
          val jsonAvatar = targetUser.avatar map (Json.toJson(_)(Avatar.avatarPersistedWrites)) getOrElse (JsNull)

          Ok(Json.obj("gender" -> gender, "avatar" -> jsonAvatar))
      }
    } getOrElse { Forbidden(Json.obj("error" -> "Access denied")) }
  }

}
