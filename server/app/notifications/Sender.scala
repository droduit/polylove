package notifications

import javax.inject.Inject

import akka.actor._
import com.google.inject.assistedinject.Assisted
import play.api.{Configuration, Logger}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object Sender {

  case class SendTo(id: String, notification: Option[JsValue] = None, data: Option[JsValue] = None)

  case class FCMResult
  (message_id: Option[String], registration_id: Option[String], error: Option[String])

  case class FCMResponse
  (multicast_id: Long, success: Int, failure: Int, canonical_ids: Long, results: Option[Seq[FCMResult]])

  implicit val fcmResultFormat = Json.format[FCMResult]
  implicit val fcmResponseFormat = Json.format[FCMResponse]

  private case class BadStatusException(status: Int, response: WSResponse) extends Exception

  private case class BadResponseException(errors: Seq[(JsPath, Seq[ValidationError])], response: WSResponse) extends Exception

  private case class FCMErrorException(error: String, response: WSResponse) extends Exception

  trait Factory {
    def apply(idManager: ActorRef): Actor
  }

}

class Sender @Inject()(@Assisted idManager: ActorRef, config: Configuration, ws: WSClient) extends Actor {

  import IdManager._
  import Sender._
  import context.dispatcher

  val RetryAfterMax = 30

  val baseUrl = config.getString("fcm.baseUrl").get
  val authHeader = "Authorization" -> ("key=" + config.getString("fcm.key").get)
  val contentHeader = "Content-Type" -> "application/json"

  def notificationAsJson(notif: SendTo) = Json.obj(
    "to" -> notif.id, "data" -> notif.data, "notification" -> notif.notification
  )

  def sendNotification(notification: SendTo): Future[WSResponse] =
    ws.url(baseUrl + "/fcm/send")
      .withHeaders(authHeader, contentHeader)
      .post(notificationAsJson(notification))

  def retry(retryAfter: Int, notification: SendTo) = {
    if (retryAfter > RetryAfterMax) {
      Logger.error(s"Unable to send the following notification: $notification")
      self ! PoisonPill
    } else {
      Logger.debug(s"Retrying to send after $retryAfter seconds: $notification")
      context become Send(retryAfter + retryAfter)
      context.system.scheduler.scheduleOnce(
        retryAfter seconds, self, notification)
    }
  }

  def retryWithResponse(response: WSResponse, retryAfter: Int, notification: SendTo) =
    retry(response.header("Retry-After").map(_.toInt).getOrElse(retryAfter), notification)

  def receive = Send(1)

  def Send(retryAfter: Int): Receive = {

    case notification@SendTo(id, _, _) =>
      Logger.debug(s"Sending notification: $notification")

      sendNotification(notification) map { response =>
        Logger.debug("Received response")
        if (response.status != 200)
          throw BadStatusException(response.status, response)

        Logger.debug("Status OK")
        val fcmResponse = response.json.validate[FCMResponse].fold(
          errors => throw BadResponseException(errors, response),
          fcmResponse => fcmResponse
        )

        Logger.debug("Response Ok")
        if (fcmResponse.failure != 0 || fcmResponse.canonical_ids != 0) {
          for {result <- fcmResponse.results.get} result match {
            case FCMResult(Some(_), Some(rid), _) =>
              idManager ! RenewId(id, rid)
              self ! PoisonPill

            case FCMResult(_, _, Some(error@"Unavailable")) =>
              throw FCMErrorException(error, response)

            case FCMResult(_, _, Some(error)) =>
              idManager ! RemoveId(id)
              throw FCMErrorException(error, response)
          }
        }

        Logger.debug("Everything is fine, bye bye!")
        self ! PoisonPill
      } recover {
        case BadStatusException(status, response) =>
          Logger.error(s"Bad status '$status' returned: $notification")
          retryWithResponse(response, retryAfter, notification)

        case BadResponseException(errors, response) =>
          val body = response.body
          Logger.error(s"FCM returned invalid response '$errors' '$body': $notification")
          retryWithResponse(response, retryAfter, notification)

        case FCMErrorException("Unavailable", response) =>
          Logger.error(s"FCM is unavailable: $notification")
          retryWithResponse(response, retryAfter, notification)

        case FCMErrorException(error, response) =>
          Logger.error(s"FCM returned an error '$error': $notification")
          self ! PoisonPill
      }

  }

}
