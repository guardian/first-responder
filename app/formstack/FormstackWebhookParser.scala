package formstack

import play.api.mvc.AnyContent

object FormstackWebhookParser {

  case class Payload(handshakeKey: String, body: String, attachmentUrl: Option[String])

  /*
  TODO we should alter the webhook registration to include the form field types,
  as we can't rely on the field names (because people might manually edit the form).
   */
  def parse(body: AnyContent): Either[String, Payload] = body.asFormUrlEncoded.fold[Either[String, Payload]] {
    Left("Failed to parse Formstack Webhook as a URL-encoded form")
  } { form =>
    def firstValue(key: String): Either[String, String] =
      form.get(key).flatMap(_.headOption).toRight(s"Missing $key field")
    for {
      handshakeKey <- firstValue("HandshakeKey").right
      body <- firstValue("Tell me something I don't know").right
    } yield {
      val attachmentUrl = form.get("Give up the goods").flatMap(_.headOption).filter(_.trim.nonEmpty)
      Payload(handshakeKey, body, attachmentUrl)
    }
  }

}
