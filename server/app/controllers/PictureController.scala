package controllers

import javax.inject._

import models._
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import sorm.Persisted

import java.io.File

/**
  * Created by thierry on 08.10.16.
  */
class PictureController @Inject()(config: Configuration) extends Controller with Auth {
  private val picturesPath = config.getString("picturesPath").get
  private def pictureForUser(user: User with Persisted) =
    new File(s"$picturesPath/${user.id}.jpg")

  def store = Authenticated(parse.temporaryFile) { implicit request =>
    if (!request.contentType.equals(Some("image/jpeg"))) {
      BadRequest(Json.obj("error" -> "Picture must be a JPEG"))
    } else {
      request.body.moveTo(pictureForUser(request.user))
      Created(Json.obj())
    }
  }

  def fetchOwn = Authenticated { implicit request =>
    val pictureFile = pictureForUser(request.user)

    if (!pictureFile.exists)
      NotFound(Json.obj("error" -> "No picture for this user"))
    else
      Ok.sendFile(content = pictureFile, inline = true)
  }

  def fetch(targetId: Long) = Authenticated { implicit request =>
    DB.query[User].whereEqual("id", targetId).fetchOne map { targetUser =>
      Match between (request.user, targetUser, Set(State.Open)) match {
        case None => Forbidden(Json.obj("error" -> "Access denied"))
        case Some(_) =>
          val pictureFile = pictureForUser(targetUser)

          if (!pictureFile.exists)
            NotFound(Json.obj("error" -> "No picture for this user"))
          else
            Ok.sendFile(content = pictureFile, inline = true)
      }
    } getOrElse { Forbidden(Json.obj("error" -> "Access denied")) }
  }

}
