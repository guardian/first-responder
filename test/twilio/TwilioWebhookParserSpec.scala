package twilio

import org.scalatest.{ Matchers, FlatSpec }
import play.api.test.FakeRequest
import twilio.TwilioWebhookParser._

class TwilioWebhookParserSpec extends FlatSpec with Matchers {

  it should "correctly parse a valid Twilio payload" in {
    val request = FakeRequest().withFormUrlEncodedBody(
      "From" -> "+447487700700",
      "Body" -> "refugeecrisis happening now"
    )
    TwilioWebhookParser.parse(request.body) should be(Right(
      Payload(
        from = "+447487700700",
        body = "refugeecrisis happening now")
    ))
  }
}
