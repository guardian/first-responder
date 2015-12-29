package controllers

import formstack.FormCreator
import models.Callout
import play.api.mvc.{ Result, Action, Controller }
import store.Dynamo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Api(validApiKey: String, dynamo: Dynamo, formCreator: FormCreator) extends Controller {

  def createCallout(apiKey: String) = Action.async { implicit request =>
    if (apiKey != validApiKey)
      Future.successful(BadRequest("Invalid API key"))
    else {
      Forms.createCalloutForm.bindFromRequest.fold[Future[Result]]({ formWithErrors =>
        Future.successful(BadRequest("Invalid POST data"))
      }, { createCalloutData =>
        formCreator.createForm(createCalloutData.hashtag) map { formstackId =>
          val callout = Callout(hashtag = createCalloutData.hashtag, description = createCalloutData.description, formstackId = Some(formstackId))
          dynamo.save(callout)
          Created
        }
      })
    }
  }

}
