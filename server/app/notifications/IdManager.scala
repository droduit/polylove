package notifications

import akka.actor._
import models.{DB, User}
import play.api.Logger

object IdManager {
  def props = Props[IdManager]

  case class RenewId(oldId: String, newId: String)

  case class RemoveId(oldId: String)

}

class IdManager extends Actor {

  import IdManager._

  def userByFirebaseId(id: String): Option[User] =
    DB.query[User].whereContains("firebaseIds", id).fetchOne

  def receive = {
    case RenewId(oldId, newId) =>
      Logger.debug(s"RenewId: '$oldId' -> '$newId'")
      userByFirebaseId(oldId)
        .map(u => u copy (firebaseIds = u.firebaseIds - oldId + newId))
        .map(DB save _)

    case RemoveId(oldId) =>
      Logger.debug(s"RemoveId: '$oldId'")
      userByFirebaseId(oldId)
        .map(u => u copy (firebaseIds = u.firebaseIds - oldId))
        .map(DB save _)
  }
}
