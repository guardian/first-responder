package formstack

import formstack.FormstackWebhookParser.Payload
import org.scalatest.{ FlatSpec, Matchers }
import play.api.test.FakeRequest

import scala.collection.mutable.ArrayBuffer

class FormstackWebhookParserSpec extends FlatSpec with Matchers {

  it should "correctly parse a Formstack webhook payload" in {
    val request = FakeRequest().withFormUrlEncodedBody(
      "A text field" ->
        """value = Here is some text
          |field_type = textarea""".stripMargin,
      "Another text field" -> "value = Here is some more text field_type = text",
      "A file field" -> "value = https://s3.amazonaws.com/files.formstack.com/uploads/2119710/35738478/213573841/35738478_cat-matlock-derbyshire.jpg field_type = file",
      "Email" -> "value = chris.birchall@theguardian.com field_type = email",
      "HandshakeKey" -> "dev"
    )
    FormstackWebhookParser.parse(request.body) should be(Right(
      Payload(
        handshakeKey = "dev",
        textFields = Map("A text field" -> "Here is some text", "Another text field" -> "Here is some more text"),
        email = Some("chris.birchall@theguardian.com"),
        attachmentUrls = Seq("https://s3.amazonaws.com/files.formstack.com/uploads/2119710/35738478/213573841/35738478_cat-matlock-derbyshire.jpg")
      )
    ))
  }

}
