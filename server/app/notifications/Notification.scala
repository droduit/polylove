package notifications

import javax.inject._

import akka.actor._
import models.User
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json._

object Notification {

  case class SendTo(user: User, notification: Option[JsValue] = None, data: Option[JsValue] = None)
}

class Notification @Inject()(senderFactory: Sender.Factory) extends Actor with InjectedActorSupport {

  import Notification._

  val idManager = context.actorOf(IdManager.props)

  def createSender: ActorRef =
    injectedChild(senderFactory(idManager), java.util.UUID.randomUUID.toString)

  def receive = {
    case SendTo(user, notif, data) =>
      user.firebaseIds foreach
        (createSender ! Sender.SendTo(_, notif, data))
  }
}
