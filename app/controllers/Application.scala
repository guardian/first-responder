package controllers

import com.gu.googleauth.GoogleAuthConfig
import formstack.FormstackEmbedder
import models._
import play.api.mvc._
import play.twirl.api.Html
import store.Dynamo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Application(dynamo: Dynamo, formstackEmbedder: FormstackEmbedder, val authConfig: GoogleAuthConfig) extends Controller with AuthActions {

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
