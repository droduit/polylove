package controllers

import javax.inject._

import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import sorm.Persisted

import models._

/**
  * Created by thierry on 08.10.16.
  */
class ProfileController extends Controller with Auth {

  def store = Authenticated(parse.json) { implicit request =>
    request.body.validate[Profile].fold(
      errors => {
        BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      },
      profile => {
        request.user.profile match {
          case None =>
            val storedProfile = DB save (profile copy (user = Some(request.user)))

            Created(Json.toJson(storedProfile)(Profile.profilePersistedWrites))

          case Some(oldProfile) =>
            val storedProfile = DB save (oldProfile copy (
              section = profile.section,
              gender = profile.gender,
              birthday = profile.birthday,
              languages = profile.languages,
              hobbies = profile.hobbies,
              description = profile.description,
              genderInterest = profile.genderInterest,
              ageInterest = profile.ageInterest
            ))

            Ok(Json.toJson(storedProfile)(Profile.profilePersistedWrites))
        }
      }
    )
  }

  def fetch(targetId: Long) = Authenticated { implicit request =>
    DB.query[User].whereEqual("id", targetId).fetchOne map { targetUser =>
      Match between (request.user, targetUser, Set(State.Open)) match {
        case None => Forbidden(Json.obj("error" -> "Access denied"))
        case Some(_) =>
          val jsonProfile = targetUser.profile map (Json.toJson(_)(Profile.profilePersistedWrites)) getOrElse (JsNull)

          Ok(jsonProfile)
      }
    } getOrElse { Forbidden(Json.obj("error" -> "Access denied")) }
  }

}
