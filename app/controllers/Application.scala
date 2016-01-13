package controllers

import com.gu.googleauth.GoogleAuthConfig
import formstack.{ FormCreator, FormstackEmbedder }
import models._
import play.api.i18n.{ MessagesApi, I18nSupport }
import play.api.mvc._
import play.twirl.api.Html
import store.Dynamo
import twilio.TwilioWebhookHandler

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Application(dynamo: Dynamo, formstackEmbedder: FormstackEmbedder, formCreator: FormCreator, val messagesApi: MessagesApi, val authConfig: GoogleAuthConfig, phoneNumber: String) extends Controller with AuthActions
    with I18nSupport {

  def index = AuthAction { implicit request =>
    val callouts = dynamo.findCallouts()
    val contributions = dynamo.findLatestJustInContributions()
    Ok(views.html.index(callouts, None, contributions, ModerationStatus.JustIn))
  }

  def showCalloutJustIn(hashtag: String) = showCallout(hashtag, ModerationStatus.JustIn)

  def showCallout(hashtag: String, status: ModerationStatus) = AuthAction { implicit request =>
    val callouts = dynamo.findCallouts()
    val contributions = dynamo.findContributionsByHashtagAndStatus(hashtag, status)
    Ok(views.html.index(callouts, Some(hashtag), contributions, status))
  }

  def createCalloutPage = AuthAction { implicit request =>
    val callouts = dynamo.findCallouts()

    Ok(views.html.create_callout(callouts, Forms.createCalloutForm))
  }

  def createCallout = AuthAction.async { implicit request =>

    Forms.createCalloutForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.create_callout(dynamo.findCallouts(), formWithErrors)))
      },
      calloutForm => {
        formCreator.createForm(calloutForm.hashtag) map { formstackId =>
          val callout = Callout.create(hashtag = calloutForm.hashtag, description = calloutForm.description, formstackId = Some(formstackId))
          dynamo.save(callout)
          Redirect(routes.Application.showCalloutJustIn(calloutForm.hashtag)).flashing("info" -> "Successfully created a new callout!")
        }
      }
    )
  }

  def showContribution(hashtag: String, id: String) = AuthAction { implicit request =>
    dynamo.findContribution(hashtag, id) match {
      case Some(contribution) =>
        val callouts = dynamo.findCallouts()
        Ok(views.html.contribution(callouts, contribution))
      case None => NotFound
    }

  }

  def updateModerationStatus(hashtag: String, id: String, status: ModerationStatus) = AuthAction { request =>
    /*
    1. Update the item in DynamoDB
    2. Find the next item in the list of contributions
    3. If there is a next item, redirect to that item's page with a flash message
       Otherwise, redirect to the updated item's page with a different flash message
    */
    dynamo.findContribution(hashtag: String, id: String) match {
      case Some(contribution) =>
        dynamo.updateModerationStatus(contribution, status)
        val nextContrib = dynamo.findNextContributionOlderThan(contribution)
        nextContrib match {
          case Some(next) =>
            Redirect(routes.Application.showContribution(hashtag, next.id)).flashing("info" -> "Successfully updated contribution")
          case None =>
            Redirect(routes.Application.showContribution(hashtag, id)).flashing("info" -> "You're all done!")
        }
      case None => NotFound
    }
  }

  def updateNotes(hashtag: String, id: String) = AuthAction { request =>
    val notes: Option[String] = for {
      form <- request.body.asFormUrlEncoded
      field <- form.get("notes")
      notes <- field.headOption
    } yield {
      notes
    }
    notes match {
      case Some(n) => dynamo.findContribution(hashtag: String, id: String) match {
        case Some(contribution) =>
          dynamo.updateNotes(contribution, n)

          Redirect(routes.Application.showContribution(hashtag, id)).flashing("info" -> "Successfully saved your notes")
        case None => NotFound
      }
      case None => BadRequest("Missing notes field")
    }

  }

  def healthcheck = Action {
    // TODO check DynamoDB health?
    Ok("OK")
  }

  // Public widget test page
  def showCalloutWidget(hashtag: String) = Action.async { request =>
    dynamo.findCalloutByHashtag(hashtag) match {
      case Some(callout) =>
        val formstackEmbed: Future[Option[Html]] = callout.formstackId.fold[Future[Option[Html]]](Future.successful(None)) { id =>
          formstackEmbedder.getEmbedCode(id).map(Some(_))
        }
        formstackEmbed.map { embed =>
          Ok(views.html.callout_widget(callout, embed, phoneNumber = phoneNumber))
        }
      case None => Future.successful(NotFound)
    }
  }
}
