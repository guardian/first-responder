package controllers

import com.gu.googleauth.GoogleAuthConfig
import models._
import play.api.mvc._
import org.joda.time.DateTime
import store.Dynamo

class Application(dynamo: Dynamo, val authConfig: GoogleAuthConfig) extends Controller with AuthActions {

  def index = AuthAction { request =>
    val callouts = dynamo.findCallouts()
    Ok(views.html.index("", callouts, Nil))
  }

  def showCallout(hashtag: String) = AuthAction { request =>
    val callouts = dynamo.findCallouts()
    val contributions = dynamo.findContributionsByHashtag(hashtag)
    Ok(views.html.index(hashtag, callouts, contributions))
  }

  def showContribution(hashtag: String, id: String) = AuthAction { request =>
    dynamo.findContribution(hashtag, id) match {
      case Some(contribution) =>
        val callouts = dynamo.findCallouts()
        Ok(views.html.contribution(hashtag, callouts, contribution))
      case None => NotFound
    }

  }

  def healthcheck = Action {
    // TODO check DynamoDB health?
    Ok("OK")
  }

  // Public widget test page
  def showCalloutWidget(hashtag: String) = Action { request =>
    dynamo.findCalloutByHashtag(hashtag) match {
      case Some(callout) =>
        Ok(views.html.callout_widget(callout))
      case None => NotFound
    }
  }

}
