package models

import org.joda.time.DateTime
import play.api.libs.json.{Json, _}
import play.api.libs.functional.syntax._
import sorm.Persisted

/**
  * Created by Dominique on 01.10.16.
  */
object Gender extends Enumeration {
  type Gender = Value
  val Male, Female, Other = Value

  implicit val genderReads = new EnumReads[Gender.type](Gender)
}

object GenderInterest extends Enumeration {
  type GenderInterest = Value
  val Male, Female, Both = Value

  implicit val genderInterestReads = new EnumReads[GenderInterest.type](GenderInterest)
}

object Language extends Enumeration {
  type Language = Value
  val Albanian, Arabic, Bulgarian, Catalan, Chinese, Croatian, Czech, Danish, Dutch, English, Estonian, Filipino, Finnish, French, Georgian, German, Greek, Hebrew, Hungarian, Icelandic, Italian, Latvian, Lithuanian, Macedonian, Malagasy, Maltese, Norwegian, Polish, Portuguese, Romanian, Russian, Korean, Japanese, Serbian, Slovak, Slovenian, Spanish, Swedish, Thai, Turkish, Ukrainian, Vietnamese = Value

  implicit val languageReads = new EnumReads[Language.type](Language)
}

object Section extends Enumeration {
  type Section = Value
  val AR, GC, SIE, CGC, MA, PH, EL, GM, MX, MT, IN, SC, STV, CMS = Value

  implicit val sectionReads = new EnumReads[Section.type](Section)
}

case class Profile (
  user: Option[User],
  section: Section.Value,
  gender: Gender.Value,
  birthday: DateTime,

  languages: Set[Language.Value],
  hobbies: Set[String],
  description: String,

  genderInterest: GenderInterest.Value,
  ageInterest: Range
)

object Profile {
  implicit val rangeFormat = new Format[Range] {
    def writes(range: Range): JsValue = Json.obj(
      "start" -> range.start,
      "end" -> range.end
    )

    def reads(rangeObj: JsValue): JsResult[Range] =
      ((rangeObj \ "start").asOpt[Int], (rangeObj \ "end").asOpt[Int]) match {
        case (Some(start), Some(end)) => JsSuccess( start to end )
        case _ => JsError("error.path.missing")
      }
  }

  implicit val profileFormat = Json.format[Profile]
  implicit val profilePersistedWrites: Writes[Profile with Persisted] = (
    (JsPath \ "id").write[Long] and
    (JsPath \ "section").write[Section.Value] and
    (JsPath \ "gender").write[Gender.Value] and
    (JsPath \ "birthday").write[DateTime] and
    (JsPath \ "languages").write[Set[Language.Value]] and
    (JsPath \ "hobbies").write[Set[String]] and
    (JsPath \ "description").write[String] and
    (JsPath \ "genderInterest").write[GenderInterest.Value] and
    (JsPath \ "ageInterest").write[Range]
  )(unlift(Profile.unapplyPersisted))

  def unapplyPersisted(p: Profile with Persisted) = {
    Some((
      p.id, p.section, p.gender, p.birthday,
      p.languages, p.hobbies, p.description,
      p.genderInterest, p.ageInterest
    ))
  }
}
