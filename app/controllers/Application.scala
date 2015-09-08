package controllers

import com.gu.googleauth.GoogleAuthConfig
import models._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Application(val authConfig: GoogleAuthConfig) extends Controller with AuthActions {

  def index = AuthAction { request =>
    Ok(views.html.index(request.user.firstName))
  }

  def healthcheck = Action {
    // TODO check DynamoDB health?
    Ok("OK")
  }

}
