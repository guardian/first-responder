package mailgun

import mailgun.MailgunWebhookParser._
import org.scalatest.{ FlatSpec, Matchers }
import play.api.test.FakeRequest

class MailgunWebhookParserSpec extends FlatSpec with Matchers {

  it should "correctly parse a valid Mailgun payload" in {
    val request = FakeRequest().withFormUrlEncodedBody(
      "sender" -> "chris.birchall@guardian.co.uk",
      "attachments" -> "[{\"url\": \"https://api.mailgun.net/v2/domains/sandboxb581b83d5fc3420982d2af09f3b2f7df.mailgun.org/messages/WyJjOWJhN2Y3NDNmIiwgWyI2N2UzYzdmNS02OGIzLTRhNTctOTQ0ZC1kN2ZkMWNhZTFiYTMiLCAiYTZhY2U1MjEtYjU3ZS00NWU1LWI1ODktNTA0MDQwNzUyN2M2Il0sICJtYWlsZ3VuIiwgInRob3IiXQ==/attachments/0\", \"content-type\": \"image/png\", \"name\": \"Screen Shot 2015-08-24 at 13.01.57.png\", \"size\": 161081}]",
      "body-plain" -> "Hello [image: Screen Shot 2015-08-24 at 13.01.57.png]",
      "subject" -> "Here's an inline attachment",
      "recipient" -> "chris@sandboxb581b83d5fc3420982d2af09f3b2f7df.mailgun.org"
    )
    MailgunWebhookParser.parse(request.body) should be(Right(
      Payload(
        from = "chris.birchall@guardian.co.uk",
        to = "chris@sandboxb581b83d5fc3420982d2af09f3b2f7df.mailgun.org",
        subject = Some("Here's an inline attachment"),
        body = "Hello [image: Screen Shot 2015-08-24 at 13.01.57.png]",
        attachments = Seq(AttachmentInfo("https://api.mailgun.net/v2/domains/sandboxb581b83d5fc3420982d2af09f3b2f7df.mailgun.org/messages/WyJjOWJhN2Y3NDNmIiwgWyI2N2UzYzdmNS02OGIzLTRhNTctOTQ0ZC1kN2ZkMWNhZTFiYTMiLCAiYTZhY2U1MjEtYjU3ZS00NWU1LWI1ODktNTA0MDQwNzUyN2M2Il0sICJtYWlsZ3VuIiwgInRob3IiXQ==/attachments/0", "image/png", 161081)))
    ))

  }
}
