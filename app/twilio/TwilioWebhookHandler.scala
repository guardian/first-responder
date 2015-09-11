package twilio

import models._
import play.api.Logger
import store.Dynamo
import twilio.TwilioWebhookParser.Payload

class TwilioWebhookHandler(dynamo: Dynamo) {

  def handlePayload(payload: Payload): Contribution = {
    val contribution = Contribution(
      hashtag = extractHashtag(payload),
      contributor = Contributor(email = None, phone = Some(payload.from)),
      channel = Channel.SMS,
      subject = None,
      body = payload.body,
      attachments = Nil,
      notes = None
    )
    dynamo.save(contribution)
    contribution
  }

  /**
   * Use the first word of the SMS body, lowercased, as the hashtag. If body is empty, log an error and use an empty string
   */
  private def extractHashtag(payload: Payload) = {
    val hashtag = payload.body.takeWhile(_ != ' ').toLowerCase
    if (hashtag.isEmpty)
      Logger.warn(s"Failed to extract a hashtag from SMS body. From: ${payload.from}, Body: [${payload.body}]")
    hashtag
  }

}
