package controllers

import mailgun.{ MailgunWebhookHandler, MailgunWebhookParser }
import play.api.Logger
import play.api.mvc.{ Result, Action, Controller }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Webhooks(mailgunApiKey: String, mailgunWebhookHandler: MailgunWebhookHandler) extends Controller {

  def mailgun(key: String) = Action.async { request =>
    if (key != mailgunApiKey)
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

}

