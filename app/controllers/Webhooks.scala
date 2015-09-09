package controllers

import com.twilio.sdk.verbs.{ Message, TwiMLResponse }
import mailgun.{ MailgunWebhookHandler, MailgunWebhookParser }
import play.api.Logger
import play.api.mvc.{ Result, Action, Controller }
import twilio.{ TwilioWebhookHandler, TwilioWebhookParser }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Webhooks(webhooksKey: String, mailgunWebhookHandler: MailgunWebhookHandler, twilioWebhookHandler: TwilioWebhookHandler) extends Controller {

  def mailgun(key: String) = Action.async { request =>
    if (key != webhooksKey)
      Future.successful(BadRequest("Invalid API key"))
    else {
      MailgunWebhookParser.parse(request.body).fold[Future[Result]]({ error =>
        Logger.warn(s"Failed to parse Mailgun webhook! Error: $error, Request: $request")
        Future.successful(InternalServerError("Sorry, you didn't send me what I was expecting"))
      }, { payload =>
        mailgunWebhookHandler.handlePayload(payload).map { contribution =>
          Logger.info(s"Received a contribution via Mailgun. ${contribution.id}, ${contribution.hashtag}, ${contribution.contributor.email}")
          Ok("Handled payload")
        }
      })
    }
  }

  def twilio(key: String) = Action { request =>
    if (key != webhooksKey)
      BadRequest("Invalid API key")
    else {
      TwilioWebhookParser.parse(request.body).fold[Result]({ error =>
        Logger.warn(s"Failed to parse Twilio webhook! Error: $error, Request: $request")
        InternalServerError("Sorry, you didn't send me what I was expecting")
      }, { payload =>
        val contribution = twilioWebhookHandler.handlePayload(payload)
        Logger.info(s"Received a contribution via Twilio. ${contribution.id}, ${contribution.hashtag}, ${contribution.contributor.phone}")
        val reply = new TwiMLResponse()
        reply.append(new Message("Thanks for your message. We may contact you to verify the information you supplied."))
        Ok(reply.toXML).as(XML)
      })
    }
  }

}

