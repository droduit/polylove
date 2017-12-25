package controllers

import org.joda.time.DateTime
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc._
import models._
import sorm.Dsl._
import sorm.Persisted

/**
  * Created by thierry on 01.10.16.
  */
class MatchController extends Controller with Auth {

  def fetchMatches = Authenticated { implicit request =>
    val matches = DB.query[Match].where(
      ("user1" equal request.user) or ("user2" equal request.user)
    ).fetch map {
      Json.toJson(_)(Match.matchPersistedWrites)
    }

    Ok(Json.obj("matches" -> matches))
  }

  case class InterestedContent(interested: Boolean)
  implicit val jsonInterestedContent: Reads[InterestedContent] = Json.reads[InterestedContent]

  def validateMatch(matchId: Long) = Authenticated(parse.json) { implicit request =>
    request.body.validate[InterestedContent].fold(
      errors => {
        BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
      },
      interest => {
        DB.query[Match].where(
          ("id" equal matchId) and
          (("user1" equal request.user) or ("user2" equal request.user)) and
          ("state" equal State.Pending)
        ).fetchOne map { discussion =>
          if (request.user equals discussion.user1)
            DB save (discussion copy (user1Interested = interest.interested))
          else
            DB save (discussion copy (user2Interested = interest.interested))

          Ok(Json.obj())
        } getOrElse {
          Forbidden(Json.obj("error" -> "No pending match found"))
        }
      }
    )
  }
}
