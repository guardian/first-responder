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

    val contributions = Seq(
      new Contribution("1", new Contributor(Some("chris.wilk@guardian.co.uk")), "refugeecrisis", "Tester", Nil),
      new Contribution("2", new Contributor(Some("chris.wilk@guardian.co.uk")), "refugeecrisis", "Tester 2", Nil)
    )

    Ok(views.html.index(callouts, contributions))
  }

  def healthcheck = Action {
    // TODO check DynamoDB health?
    Ok("OK")
  }

}
