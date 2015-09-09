package controllers

import com.gu.googleauth.GoogleAuthConfig
import models._
import play.api.mvc._
import org.joda.time.DateTime

class Application(val authConfig: GoogleAuthConfig) extends Controller with AuthActions {

  def index = AuthAction { request =>

    val callouts = Seq(new Callout("refugeecrisis", DateTime.now, ""), new Callout("bangkokbombing", DateTime.now, ""))

    Ok(views.html.index("", callouts, Nil))
  }

  def showCallout(hashtag: String) = AuthAction { request =>

    // Stub callouts and contributions
    val callouts = Seq(new Callout("refugeecrisis", DateTime.now, ""), new Callout("bangkokbombing", DateTime.now, ""))

    val contributions = Seq(
      new Contribution("refugeecrisis", "1", new Contributor(Some("chris.wilk@guardian.co.uk")), Channel.Mail, DateTime.now, None, "Tester", Nil),
      new Contribution("refugeecrisis", "2", new Contributor(Some("chris.wilk@guardian.co.uk")), Channel.Form, DateTime.now.minusMinutes(1), None, "Tester 2", Nil),
      new Contribution("refugeecrisis", "2", new Contributor(Some("chris.wilk@guardian.co.uk")), Channel.SMS, DateTime.now.minusMinutes(2), None, "Tester 3", Nil)
    )

    Ok(views.html.index(hashtag, callouts, contributions))
  }

  def showContribution(hashtag: String, id: Int) = AuthAction { request =>

    // Stub callouts and contributions
    val callouts = Seq(new Callout("refugeecrisis", DateTime.now, ""), new Callout("bangkokbombing", DateTime.now, ""))
    val contribution = new Contribution("refugeecrisis", "1", new Contributor(Some("chris.wilk@guardian.co.uk")), Channel.Mail, DateTime.now, None, "Tester", Nil)

    Ok(views.html.contribution(hashtag, callouts, contribution))
  }

  def healthcheck = Action {
    // TODO check DynamoDB health?
    Ok("OK")
  }

}
