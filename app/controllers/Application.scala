package controllers

import com.gu.googleauth.GoogleAuthConfig
import models._
import play.api.mvc._
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Application(val authConfig: GoogleAuthConfig) extends Controller with AuthActions {

  def index = AuthAction { request =>

    // Stub callouts and contributions
    val callouts = Seq(new Callout("refugeecrisis", DateTime.now, ""), new Callout("bangkokbombing", DateTime.now, ""))

    Ok(views.html.index(callouts, Nil))
  }

  def healthcheck = Action {
    // TODO check DynamoDB health?
    Ok("OK")
  }

}
