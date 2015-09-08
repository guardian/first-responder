package controllers

import mailgun.MailgunWebhookParser
import play.api.Logger
import play.api.mvc.{ Result, Action, Controller }

import scala.concurrent.Future

class Webhooks(mailgunApiKey: String) extends Controller {

  def mailgun(key: String) = Action.async { request =>
    if (key != mailgunApiKey)
      Future.successful(BadRequest("Invalid API key"))
    else {
      MailgunWebhookParser.parse(request.body).fold[Future[Result]]({ error =>
        Logger.warn(s"Failed to parse Mailgun webhook! Error: $error, Request: $request")
        Future.successful(InternalServerError("Sorry, you didn't send me what I was expecting"))
      }, { payload =>
        // TODO handle the payload: fetch attachments, store everything to Dynamo and S3
        Future.successful(Ok("TODO"))
      })
    }
  }

}

