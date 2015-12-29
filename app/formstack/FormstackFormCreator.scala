package formstack

import play.api.libs.json._
import play.api.libs.ws.WSAPI

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait FormCreator {

  /**
   * Register a new form for a callout.
   * The real implementation uses the FormStack API to do so.
   *
   * @param hashtag the callout's hashtag
   * @return the ID of the form that was created
   */
  def createForm(hashtag: String): Future[String]

}

class FormstackFormCreator(ws: WSAPI, webhookKey: String, formstackOAuthToken: String, baseUrl: String) extends FormCreator {

  /**
   * Use the Formstack API to register a new form and corresponding Webhook.
   * Formstack will send us a Webhook POST every time somebody submits the form.
   *
   * @return the Formstack ID of the form
   */
  def createForm(hashtag: String): Future[String] = {
    ws.url(s"https://www.formstack.com/api/v2/form.json?oauth_token=$formstackOAuthToken")
      .withHeaders("Content-Type" -> "application/json")
      .post(standardForm(hashtag))
      .map { response =>
        (response.json \ "id").as[String]
      }
  }

  private def standardForm(hashtag: String): JsValue = {
    Json.obj(
      "name" -> s"Breaking news - $hashtag",
      "fields" -> Json.arr(
        Json.obj(
          "field_type" -> "email",
          "label" -> "Email"
        ),
        Json.obj(
          "field_type" -> "textarea",
          "label" -> "Tell me something I don't know"
        ),
        Json.obj(
          "field_type" -> "file",
          "label" -> "Give up the goods",
          "attributes" -> Json.obj(
            "types" -> "jpg,jpeg,gif,png,bmp,txt,mp3,mp4,aac,wav,au,wmv,avi,mpg,mpeg"
          )
        )
      ),
      "webhooks" -> Json.arr(
        Json.obj(
          "url" -> s"$baseUrl/webhooks/formstack/$hashtag",
          "handshake_key" -> webhookKey,
          "include_field_type" -> true
        )
      )
    )
  }
}

/**
 * Form creator for use in dev environment because we won't want to spam FormStack.
 */
class DummyFormCreator extends FormCreator {

  def createForm(hashtag: String): Future[String] = Future.successful("dummy-123123123")

}
