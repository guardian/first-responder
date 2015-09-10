package controllers

import formstack.Formstack
import models.Callout
import play.api.mvc.{ Action, Controller }
import store.Dynamo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Api(validApiKey: String, dynamo: Dynamo, formstack: Formstack) extends Controller {

  def createCallout(apiKey: String) = Action.async { request =>
    if (apiKey != validApiKey)
      Future.successful(BadRequest("Invalid API key"))
    else {
      request.body.asFormUrlEncoded.fold(Future.successful(BadRequest("Failed to parse POST body as a form"))) { form =>
        val hashtag = for {
          values <- form.get("hashtag")
          head <- values.headOption
        } yield head
        hashtag match {
          case Some(ht) =>
            formstack.createForm(ht) map { formstackId =>
              // TODO add formstack form ID to Callout
              val description = form.get("description").flatMap(_.headOption)
              val callout = Callout(hashtag = ht, description = description)
              dynamo.save(callout)
              Created
            }
          case None =>
            Future.successful(BadRequest("Missing hashtag field"))
        }
      }
    }
  }

}
