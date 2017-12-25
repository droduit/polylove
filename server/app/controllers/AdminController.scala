package controllers

import javax.inject._

import akka.actor._
import matcher._
import models._
import notifications.Notification
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc._
import sorm.Persisted

import services.InfosRevealer

/**
  * Created by thierry on 08.10.16.
  */
class AdminController @Inject()(infosRevealer: InfosRevealer, @Named("notification") notification: ActorRef) extends Controller {

  def fetchUsers = Action { implicit request =>
    val users = DB.query[User].fetch

    Ok(views.html.Admin.users(users))
  }

  case class UserForm(firstname: String, lastname: String, email: String, firebaseId: String)

  val userForm = Form(
    mapping(
      "firstname" -> text,
      "lastname" -> text,
      "email" -> text,
      "firebaseId" -> text
    )(UserForm.apply)(UserForm.unapply)
  )

  def storeUsers = Action { implicit request =>
    userForm.bindFromRequest.fold(
      errors => {
        Redirect("/admin/users").flashing("failure" -> "Invalid form")
      },
      user => {
        DB save User(user.email, user.firstname, user.lastname, Set(user.firebaseId))

        Redirect("/admin/users").flashing("success" -> "User created")
      }
    )
  }

  def fetchMatches = Action { implicit request =>
    val matches = DB.query[Match].fetch
    val users = DB.query[User].fetch

    Ok(views.html.Admin.matches(matches, users))
  }

  case class MatchForm(user1: Long, user2: Long)

  val matchForm = Form(
    mapping(
      "user1" -> longNumber,
      "user2" -> longNumber
    )(MatchForm.apply)(MatchForm.unapply)
  )

  def storeMatches = Action { implicit request =>
    matchForm.bindFromRequest.fold(
      errors => {
        Redirect("/admin/matches").flashing("failure" -> "Invalid form")
      },
      _match => {
        val user1 = DB.fetchById[User](_match.user1)
        val user2 = DB.fetchById[User](_match.user2)
        val newMatch = DB save Match(user1, user2, user1Interested = false, user2Interested = false, State.Pending, DateTime.now)

        val notifdata = Json.obj("subject" -> "new-match", "match" -> newMatch.id, "user1" -> user1.id, "user2" -> user2.id)

        notification ! Notification.SendTo(user1, None, Some(notifdata))
        notification ! Notification.SendTo(user2, None, Some(notifdata))

        Redirect("/admin/matches").flashing("success" -> "Match created")
      }
    )
  }

  case class MessageForm(matchId: Long, senderId: Long, content: String)

  val messageForm = Form(
    mapping(
      "matchId" -> longNumber,
      "senderId" -> longNumber,
      "content" -> text
    )(MessageForm.apply)(MessageForm.unapply)
  )

  def storeMessages = Action { implicit request =>
    messageForm.bindFromRequest.fold(
      errors => {
        Redirect("/admin/matches").flashing("failure" -> "Invalid form")
      },
      message => {
        val discussion = DB.fetchById[Match](message.matchId)
        val sender = DB.fetchById[User](message.senderId)
        val newMessage = DB save Message(discussion, Some(sender), message.content, DateTime.now)

        val notifdata = Json.obj(
          "subject" -> "new-message",
          "matchId" -> discussion.id, "senderId" -> sender.id,
          "messageId" -> newMessage.id, "content" -> newMessage.content, "sentAt" -> newMessage.sentAt
        )

        if (discussion.user1.asInstanceOf[User with Persisted].id == sender.id)
          notification ! Notification.SendTo(discussion.user2, None, Some(notifdata))
        else
          notification ! Notification.SendTo(discussion.user1, None, Some(notifdata))

        Redirect("/admin/matches").flashing("success" -> "Message sent")
      }
    )
  }

  case class CloseForm(action: String)

  val closeForm = Form(
    mapping(
      "action" -> text
    )(CloseForm.apply)(CloseForm.unapply)
  )

  def closeMatches = Action { implicit request =>
    closeForm.bindFromRequest.fold(
      errors => {
        Redirect("/admin/matches").flashing("failure" -> "Invalid form")
      },
      close => {
        val predicate = close.action match {
          case "Normal" =>
            (m: Match) => if (m.user1Interested && m.user2Interested) State.Open else State.Close
          case "Love" =>
            (m: Match) => State.Open
          case "Hate" =>
            (m: Match) => State.Close
        }

        DB.query[Match].whereEqual("state", State.Pending).fetch foreach { m =>
          val newState = predicate(m)

          DB save (m copy (state = newState))

          val notifdata = Json.obj("subject" -> "end-match", "matchId" -> m.id, "state" -> newState)

          notification ! Notification.SendTo(m.user1, None, Some(notifdata))
          notification ! Notification.SendTo(m.user2, None, Some(notifdata))
        }

        Redirect("/admin/matches").flashing("success" -> "Matches closed")
      }
    )
  }

  case class OpenForm(strategy: String)

  val openForm = Form(
    mapping(
      "strategy" -> text
    )(OpenForm.apply)(OpenForm.unapply)
  )

  def openMatches = Action { implicit request =>
    openForm.bindFromRequest.fold(
      errors => {
        Redirect("/admin/matches").flashing("failure" -> "Invalid form")
      },
      open => {
        val matches = open.strategy match {
          case "Bloom" => SmartMatcher(DB.query[User].fetch)
          case "Random" => RandomMatcher(DB.query[User].fetch)
        }

        matches map (DB save _) foreach { _match =>
          val u1 = _match.user1.asInstanceOf[User with Persisted]
          val u2 = _match.user2.asInstanceOf[User with Persisted]

          val notifdata = Json.obj("subject" -> "new-match", "match" -> _match.id, "user1" -> u1.id, "user2" -> u2.id)

          notification ! Notification.SendTo(u1, None, Some(notifdata))
          notification ! Notification.SendTo(u2, None, Some(notifdata))
        }

        Redirect("/admin/matches").flashing("success" -> "Matches opened")
      }
    )
  }

  def nuke = Action { implicit request =>
    DB.query[User].fetch.foreach(u => DB.delete(u))

    Redirect(request.headers.get("Referer").get).flashing("success" -> "Nuke done")
  }

  def reveal = Action { implicit request =>
    infosRevealer.revealTask.run()

    Redirect("/admin/matches").flashing("success" -> "Infos revealed")
  }

}
