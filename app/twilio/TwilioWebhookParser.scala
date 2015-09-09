package twilio

import play.api.mvc.AnyContent

object TwilioWebhookParser {

  case class Payload(from: String, body: String)

  def parse(request: AnyContent): Either[String, Payload] = request.asFormUrlEncoded.fold[Either[String, Payload]] {
    Left("Failed to parse Twilio Webhook as a URL-encoded form")
  } { form =>
    def firstValue(key: String): Either[String, String] =
      form.get(key).flatMap(_.headOption).toRight(s"Missing $key field")
    for {
      from <- firstValue("From").right
      body <- firstValue("Body").right
    } yield {
      Payload(from, body)
    }
  }

}
