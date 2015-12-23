package controllers

import com.gu.googleauth.GoogleAuthConfig
import formstack.{ FormCreator, FormstackFormCreator, FormstackEmbedder }
import models._
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{ MessagesApi, I18nSupport }
import play.api.mvc._
import play.twirl.api.Html
import store.Dynamo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Application(dynamo: Dynamo, formstackEmbedder: FormstackEmbedder, formstack: FormCreator, val messagesApi: MessagesApi, val authConfig: GoogleAuthConfig) extends Controller with AuthActions
    with I18nSupport {

  def index = AuthAction { request =>
    val callouts = dynamo.findCallouts()
    val contributions = dynamo.findLatestJustInContributions()
    Ok(views.html.index("", callouts, contributions, ModerationStatus.JustIn))
  }

  def showCalloutJustIn(hashtag: String) = showCallout(hashtag, ModerationStatus.JustIn)

  def showCallout(hashtag: String, status: ModerationStatus) = AuthAction { request =>
    val callouts = dynamo.findCallouts()
    val contributions = dynamo.findContributionsByHashtagAndStatus(hashtag, status)
    Ok(views.html.index(hashtag, callouts, contributions, status))
  }

  def createCalloutPage = AuthAction { implicit request =>
    val callouts = dynamo.findCallouts()

    Ok(views.html.create_callout("ccc", callouts, Some(ModerationStatus.JustIn), Application.createCalloutForm))
  }

  def createCallout = AuthAction.async { implicit request =>

    Application.createCalloutForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest("Failed to validate the form"))
      },
      calloutForm => {
        formstack.createForm(calloutForm.hashtag) map { formstackId =>
          val callout = Callout(hashtag = calloutForm.hashtag, description = calloutForm.description, formstackId = Some(formstackId))
          dynamo.save(callout)

          val callouts = dynamo.findCallouts()
          val contributions = dynamo.findContributionsByHashtagAndStatus(calloutForm.hashtag, ModerationStatus.JustIn)

          Ok(views.html.index(calloutForm.hashtag, callouts, contributions, ModerationStatus.JustIn)).flashing("info" -> "Successfully created a new callout!")
        }
      }
    )
  }

  def showContribution(hashtag: String, id: String) = AuthAction { request =>
    dynamo.findContribution(hashtag, id) match {
      case Some(contribution) =>
        val callouts = dynamo.findCallouts()
        Ok(views.html.contribution(hashtag, callouts, contribution))
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
          Ok(views.html.callout_widget(callout, embed))
        }
      case None => Future.successful(NotFound)
    }
  }
}

object Application {
  import Forms._

  case class CreateCalloutData(hashtag: String, description: Option[String])

  val createCalloutForm = Form(
    mapping(
      "hashtag" -> nonEmptyText(minLength = 2, maxLength = 45),
      "description" -> optional(text)
    )(CreateCalloutData.apply)(CreateCalloutData.unapply)
  )
}
