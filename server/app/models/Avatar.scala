package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import sorm.Persisted

/**
  * Created by Dominique on 01.10.16.
  */
object HairColor extends Enumeration {
  type HairColor = Value
  val Blond, Brown, Black, Ginger = Value

  implicit val hairReads = new EnumReads[HairColor.type](HairColor)
}

object HairStyle extends Enumeration {
  type HairStyle = Value
  val Style1, Style2, Style3, Style4, Style5, Style6, Style7 = Value

  implicit val hairReads = new EnumReads[HairStyle.type](HairStyle)
}

object Eye extends Enumeration {
  type Eye = Value
  val Blue, Green, Brown = Value

  implicit val eyeReads = new EnumReads[Eye.type](Eye)
}

object Skin extends Enumeration {
  type Skin = Value
  val Dark, Medium, Light = Value

  implicit val skinReads = new EnumReads[Skin.type](Skin)
}

object Shirt extends Enumeration {
  type Shirt = Value
  val Style1, Style2, Style3 = Value

  implicit val shirtReads = new EnumReads[Shirt.type](Shirt)
}

case class Avatar
  (user: Option[User], hairColor: HairColor.Value, hairStyle: HairStyle.Value, eyes: Eye.Value,
   skin: Skin.Value, shirt: Shirt.Value)

object Avatar {
  implicit val avatarFormat = Json.format[Avatar]
  implicit val avatarPersistedWrites: Writes[Avatar with Persisted] = (
    (JsPath \ "id").write[Long] and
    (JsPath \ "hairColor").write[HairColor.Value] and
    (JsPath \ "hairStyle").write[HairStyle.Value] and
    (JsPath \ "eyes").write[Eye.Value] and
    (JsPath \ "skin").write[Skin.Value] and
    (JsPath \ "shirt").write[Shirt.Value]
  )(unlift(Avatar.unapplyPersisted))

  def unapplyPersisted(a: Avatar with Persisted) = {
    Some((a.id, a.hairColor, a.hairStyle, a.eyes, a.skin, a.shirt))
  }
}
