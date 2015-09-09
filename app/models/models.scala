package models

import org.joda.time.{ DateTimeZone, DateTime }
import play.api.libs.json._
import enumeratum._

/**
 * A campaign/call for contributions from readers.
 *
 * Its main identifier is a hashtag (which we store without the leading hash!), e.g. "refugeecrisis".
 *
 * The corresponding email address is generated using this hashtag,
 * e.g. refugeecrisis@respond.gu.com (the mail domain is still TBC)
 */
case class Callout(
  hashtag: String,
  created: DateTime,
  description: String)

object Callout {
  implicit val jsonFormat = Json.format[Callout]

  // TODO some kind of constant "unknown/other" callout?
}

/**
 * How a response was submitted to us
 */
sealed trait Channel extends EnumEntry
object Channel extends Enum[Channel] with PlayJsonEnum[Channel] {
  val values = findValues

  case object Mail extends Channel
  case object Form extends Channel
  case object SMS extends Channel
}

case class Contributor(email: Option[String])
object Contributor {
  implicit val jsonFormat = Json.format[Contributor]

  val Anonymous = Contributor(email = None)
}

/**
 * An image/video attached to a contribution, e.g. as an email attachment
 */
case class Attachment(url: String, mimeType: String)
object Attachment {
  implicit val jsonFormat = Json.format[Attachment]
}

/**
 * Something submitted by a reader in response to a callout.
 * It may have been submitted via email, FormStack form, SMS, ...
 *
 * I wanted to call it Response but the inevitable name clashes would be too painful.
 *
 * `hashtag` is the hash key in DynamoDB, and `createdAt` (stored as a UTC ISO string) is the range key.
 *
 */
case class Contribution(
  hashtag: String,
  id: String = java.util.UUID.randomUUID.toString,
  contributor: Contributor,
  channel: Channel,
  createdAt: DateTime = DateTime.now.withZone(DateTimeZone.UTC),
  subject: Option[String],
  body: String, // TODO for FormStack, just dump all Qs and As into the body?
  attachments: Seq[Attachment])
object Contribution {
  implicit val jsonFormat = Json.format[Contribution]

}
