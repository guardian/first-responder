package controllers

import com.gu.googleauth.GoogleAuthConfig
import models._
import play.api.mvc._
import org.joda.time.DateTime
import store.Dynamo

class Application(dynamo: Dynamo, val authConfig: GoogleAuthConfig) extends Controller with AuthActions {

  def index = AuthAction { request =>

    val callouts = Seq(new Callout("refugeecrisis", DateTime.now, ""), new Callout("bangkokbombing", DateTime.now, ""))

    Ok(views.html.index("", callouts, Nil))
  }

  def showCallout(hashtag: String) = AuthAction { request =>

    // Stub callouts for now
    val callouts = Seq(new Callout("refugeecrisis", DateTime.now, ""), new Callout("bangkokbombing", DateTime.now, ""))

    val contributions = dynamo.findContributionsByHashtag(hashtag)

    Ok(views.html.index(hashtag, callouts, contributions))
  }

  def showContribution(hashtag: String, id: String) = AuthAction { request =>

    // Stub callouts for now
    val callouts = Seq(new Callout("refugeecrisis", DateTime.now, ""), new Callout("bangkokbombing", DateTime.now, ""))

    dynamo.findContribution(hashtag, id) match {
      case Some(contribution) => Ok(views.html.contribution(hashtag, callouts, contribution))
      case None => NotFound
    }

  }

  def healthcheck = Action {
    // TODO check DynamoDB health?
    Ok("OK")
  }

}
