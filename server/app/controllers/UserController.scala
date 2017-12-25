package controllers

import javax.inject._

import akka.actor._
import models._
import play.api.Logger
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import tequila.Tequila
import sorm.Persisted

import scala.concurrent.Future
import java.io.File

import tequila.Tequila

/**
  * Created by thierry on 08.10.16.
  */
class UserController @Inject()(tequila: Tequila) extends Controller with Auth {

  case class FirebaseId(id: String)
  implicit val jsonFirebaseIdFormat = Json.format[FirebaseId]

  def storeFirebaseId = Authenticated(parse.json) { implicit request =>
    request.body.validate[FirebaseId].fold(
      errors => {
        BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      },
      firebaseId => {
        val user = DB save (request.user copy (firebaseIds = request.user.firebaseIds + firebaseId.id))
        Created(Json.obj())
      }
    )
  }

  case class LoginRequest(id: String)
  implicit val jsonLoginRequestFormat = Json.format[LoginRequest]

  def login = Action.async(parse.json) { implicit request =>
    request.body.validate[LoginRequest] fold(

      // Wrong request received
      errors => Future {
        BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      },

      // Good login request received
      loginRequest => {
        tequila.fetchToken(loginRequest.id).map(_.accessToken)
          .flatMap(tequila fetchProfile _)
          .map { profile =>
            // Check if user id already present in DB
            DB.query[User].whereEqual("email", profile.email).fetchOne match {
              case Some(user) =>
                val jsonUser = Json.toJson(user)(User.userPersistedWrites)
                val jsonProfile = user.profile.map(Json.toJson(_)(Profile.profilePersistedWrites)).getOrElse(JsNull)
                val jsonAvatar = user.avatar.map(Json.toJson(_)(Avatar.avatarPersistedWrites)).getOrElse(JsNull)

                Ok(Json.obj(
                  "created" -> false, "user" -> jsonUser, "profile" -> jsonProfile, "avatar" -> jsonAvatar
                )).withSession("userId" -> user.id.toString)

              case None =>
                val newUser = DB save User(profile.email, profile.firstname, profile.name, Set())
                val jsonUser = Json.toJson(newUser)(User.userPersistedWrites)

                Created(Json.obj(
                  "created" -> true, "user" -> jsonUser, "profile" -> JsNull, "avatar" -> JsNull
                )).withSession("userId" -> newUser.id.toString)
            }
          } recover {
          case Tequila.BadStatusException(status) =>
            InternalServerError(Json.obj("error" -> "Tequila request failed"))

          case Tequila.TequilaErrorException(error) =>
            InternalServerError(Json.obj("error" -> s"Tequila error: $error"))

          case exception =>
            Logger.error(exception.getMessage)
            InternalServerError(Json.obj("error" -> "Internal error"))
        }
      }
    )
  }

  def fetch(targetId: Long) = Authenticated { implicit request =>
    DB.query[User].whereEqual("id", targetId).fetchOne map { targetUser =>
      Match between (request.user, targetUser, Set(State.Open)) match {
        case None => Forbidden(Json.obj("error" -> "Access denied"))
        case Some(_) =>
          val jsonUser = Json.toJson(targetUser)(User.userPersistedWrites)
          val jsonProfile = targetUser.profile map (Json.toJson(_)(Profile.profilePersistedWrites)) getOrElse (JsNull)
          val jsonAvatar = targetUser.avatar map (Json.toJson(_)(Avatar.avatarPersistedWrites)) getOrElse (JsNull)

          Ok(Json.obj("user" -> jsonUser, "profile" -> jsonProfile, "avatar" -> jsonAvatar))
      }
    } getOrElse { Forbidden(Json.obj("error" -> "Access denied")) }
  }

}
