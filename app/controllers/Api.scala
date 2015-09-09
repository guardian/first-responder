package controllers

import models.Callout
import play.api.mvc.{ Action, Controller }
import store.Dynamo

class Api(validApiKey: String, dynamo: Dynamo) extends Controller {

  def createCallout(apiKey: String) = Action { request =>
    if (apiKey != validApiKey)
      BadRequest("Invalid API key")
    else {
      request.body.asFormUrlEncoded.fold(BadRequest("Failed to parse POST body as a form")) { form =>
        val hashtag = for {
          values <- form.get("hashtag")
          head <- values.headOption
        } yield head
        hashtag match {
          case Some(ht) =>
            val description = form.get("description").flatMap(_.headOption)
            val callout = Callout(hashtag = ht, description = description)
            dynamo.save(callout)
            Created
          case None =>
            BadRequest("Missing hashtag field")
        }
      }
    }
  }

}
