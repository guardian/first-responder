package models

import java.util.UUID

import enumeratum.EnumEntry.Snakecase
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
  rangeKey: String,
  createdAt: DateTime,
  description: Option[String],
  formstackId: Option[String])

object Callout {
  implicit val jsonFormat = Json.format[Callout]

  /**
   * Create a new callout. The callout's `createdAt` will be set to the current time.
   */
  def create(hashtag: String, description: Option[String], formstackId: Option[String]): Callout = {
    val createdAt = DateTime.now.withZone(DateTimeZone.UTC)
    val rangeKey = s"${createdAt}_${UUID.randomUUID()}"
    Callout(hashtag, rangeKey, createdAt, description, formstackId)
  }

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

case class Contributor(email: Option[String], phone: Option[String])
object Contributor {
  implicit val jsonFormat = Json.format[Contributor]

  val Anonymous = Contributor(email = None, phone = None)
}

/**
 * An image/video attached to a contribution, e.g. as an email attachment
 */
case class Attachment(url: String, mimeType: String)
object Attachment {
  implicit val jsonFormat = Json.format[Attachment]
}

sealed trait ModerationStatus extends EnumEntry with Snakecase {
  /** A human-readable label for use in the UI */
  def label: String = toString
}
object ModerationStatus extends Enum[ModerationStatus] with PlayEnum[ModerationStatus] with PlayJsonEnum[ModerationStatus] {
  val values = findValues

  case object JustIn extends ModerationStatus { override val label = "Just in" }
  case object Selected extends ModerationStatus
  case object Discarded extends ModerationStatus
  case object Ready extends ModerationStatus
}

/**
 * Something submitted by a reader in response to a callout.
 * It may have been submitted via email, FormStack form, SMS, ...
 *
 * I wanted to call it Response but the inevitable name clashes would be too painful.
 *
 * `hashtag` is the hash key in DynamoDB, and `rangeKey` (`createdAt` concatentated with `id`) is the range key.
 *
 */
case class Contribution(
  hashtag: String,
  rangeKey: String,
  id: String,
  contributor: Contributor,
  channel: Channel,
  createdAt: DateTime,
  subject: Option[String],
  body: String,
  attachments: Seq[Attachment],
  moderationStatus: ModerationStatus,
  notes: Option[String])

object Contribution {
  implicit val jsonFormat = Json.format[Contribution]

  /**
   * Create a new contribution.
   * It will be given a randomly generated ID,
   * its createdAt will be set to the current time,
   * and it will have "just in" moderation status.
   */
  def create(
    hashtag: String,
    contributor: Contributor,
    channel: Channel,
    subject: Option[String],
    body: String,
    attachments: Seq[Attachment],
    notes: Option[String]): Contribution = {
    val id = UUID.randomUUID().toString
    val createdAt = DateTime.now.withZone(DateTimeZone.UTC)
    val rangeKey = s"${createdAt}_${id}"
    val moderationStatus = ModerationStatus.JustIn
    Contribution(hashtag, rangeKey, id, contributor, channel, createdAt, subject, body, attachments, moderationStatus, notes)
  }

}
