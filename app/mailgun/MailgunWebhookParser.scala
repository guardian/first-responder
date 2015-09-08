package mailgun

import play.api.libs.json.Json
import play.api.mvc.AnyContent

import scala.util.Try

object MailgunWebhookParser {

  case class AttachmentInfo(url: String, `content-type`: String)
  implicit val attachmentInfoReads = Json.reads[AttachmentInfo]

  case class Payload(from: String, to: String, subject: Option[String], body: String, attachments: Seq[AttachmentInfo])

  def parse(request: AnyContent): Either[String, Payload] = request.asFormUrlEncoded.fold[Either[String, Payload]] {
    Left("Failed to parse Mailgun Webhook as a URL-encoded form")
  } { form =>
    def firstValue(key: String): Either[String, String] =
      form.get(key).flatMap(_.headOption).toRight(s"Missing $key field")
    for {
      from <- firstValue("sender").right
      to <- firstValue("recipient").right
      body <- firstValue("body-plain").right
    } yield {
      val subject = form.get("subject").flatMap(_.headOption)
      val attachments = parseAttachments(form.get("attachments").flatMap(_.headOption))
      Payload(from, to, subject, body, attachments)
    }
  }

  private def parseAttachments(field: Option[String]): Seq[AttachmentInfo] = {
    (for {
      string <- field
      json <- Try(Json.parse(string)).toOption
      attachments <- json.validate[Seq[AttachmentInfo]].asOpt
    } yield attachments).getOrElse(Nil)
  }

}
