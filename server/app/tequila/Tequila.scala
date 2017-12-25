package tequila

import javax.inject._

import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.Future

object Tequila {

  case class BadStatusException(status: Int) extends Exception

  case class TequilaErrorException(error: String) extends Exception

  case class Error(error: String)

  implicit val jsonErrorReads: Reads[Error] = Json.reads[Error]

  case class TokenContainer(accessToken: String)

  implicit val jsonTokenContainerReads: Reads[TokenContainer] =
    (JsPath \ "access_token").read[String].map(TokenContainer(_))

  case class Profile(sciper: Int, username: String, email: String, firstname: String, name: String)

  implicit val jsonProfileReads: Reads[Profile] = (
    (JsPath \ "Sciper").read[Int] and
    (JsPath \ "Username").read[String] and
    (JsPath \ "Email").read[String] and
    (JsPath \ "Firstname").read[String] and
    (JsPath \ "Name").read[String]
  ) (Profile.apply _)
}

@Singleton
class Tequila @Inject()(config: Configuration, ws: WSClient) {

  import tequila.Tequila._

  private val baseUrl = config.getString("tequila.baseUrl").get
  private val clientId = config.getString("tequila.clientId").get
  private val clientSecret = config.getString("tequila.clientSecret").get
  private val redirectUri = config.getString("tequila.redirectUri").get

  def fetchToken(code: String): Future[TokenContainer] = {
    ws.url(baseUrl + "/cgi-bin/OAuth2IdP/token")
      .withQueryString(
        "grant_type" -> "authorization_code", "scope" -> "Tequila.profile",
        "client_id" -> clientId, "client_secret" -> clientSecret, "code" -> code, "redirect_uri" -> redirectUri)
      .get map { response =>
      if (response.status != 200)
        throw BadStatusException(response.status)

      response.json.validate[Error]
        .orElse(response.json.validate[TokenContainer]) match {
        case JsSuccess(container: TokenContainer, _) => container
        case JsSuccess(Error(error), _) => throw TequilaErrorException(error)
        case _ => throw TequilaErrorException("Unknown response")
      }
    }
  }

  def fetchProfile(token: String): Future[Profile] = {
    ws.url(baseUrl + "/cgi-bin/OAuth2IdP/userinfo")
      .withQueryString("access_token" -> token)
      .get map { response =>
      if (response.status != 200)
        throw BadStatusException(response.status)

      response.json.validate[Error]
        .orElse(response.json.validate[Profile]) match {
        case JsSuccess(profile: Profile, _) => profile
        case JsSuccess(Error(error), _) => throw TequilaErrorException(error)
        case _ => throw TequilaErrorException("Unknown response")
      }
    }
  }
}
