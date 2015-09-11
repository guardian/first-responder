package formstack

import play.api.Logger
import play.api.mvc.AnyContent

import scala.util.matching.Regex

object FormstackWebhookParser {

  case class Payload(handshakeKey: String, textFields: Map[String, String], email: Option[String], attachmentUrls: Seq[String])

  def parse(body: AnyContent): Either[String, Payload] = body.asFormUrlEncoded.fold[Either[String, Payload]] {
    Left("Failed to parse Formstack Webhook as a URL-encoded form")
  } { form =>
    def firstValue(key: String): Either[String, String] =
      form.get(key).flatMap(_.headOption).toRight(s"Missing $key field")
    for {
      handshakeKey <- firstValue("HandshakeKey").right
    } yield {
      val textFields = findTextFields(form)
      val email = findEmailAddresses(form).headOption
      val attachmentUrls = findAttachmentUrls(form).filter(_.trim.nonEmpty)
      Payload(handshakeKey, textFields, email, attachmentUrls.toSeq)
    }
  }

  private def findTextFields(form: Map[String, Seq[String]]): Map[String, String] = form.collect {
    case (fieldName, Seq(TextualField(value), _*)) => (fieldName, value)
  }
  private def findEmailAddresses(form: Map[String, Seq[String]]): Iterable[String] = form.collect {
    case (fieldName, Seq(EmailField(value), _*)) => value
  }
  private def findAttachmentUrls(form: Map[String, Seq[String]]): Iterable[String] = form.collect {
    case (fieldName, Seq(FileField(value), _*)) => value
  }

  private trait FieldSelector {
    def regex: Regex
    def unapply(field: String): Option[String] = regex.findFirstMatchIn(field).map(_.group(1))
  }
  private object TextualField extends FieldSelector {
    val regex =
      """^value = (.*) field_type = (text|textarea|name|address|phone|creditcard|datetime|number|select|checkbox|matrix|richtext)$""".r
  }
  private object EmailField extends FieldSelector {
    val regex =
      """^value = (.*) field_type = email$""".r
  }
  private object FileField extends FieldSelector {
    val regex =
      """^value = (.*) field_type = file$""".r
  }

}
