package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

class EnumReads[E <: Enumeration](enum: E) extends Reads[E#Value] {
  def reads(enumJson: JsValue): JsResult[E#Value] =
    enumJson
      .asOpt[String]
      .flatMap(enumName => enum.values.find(_.toString == enumName))
      .map(JsSuccess(_))
      .getOrElse(JsError("error.value.invalid"))
}
