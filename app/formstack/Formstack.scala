package formstack

import play.api.libs.json._
import play.api.libs.ws.WSAPI

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Formstack(ws: WSAPI, webhookKey: String, formstackOAuthToken: String) {

  // TODO
  def handleWebhook() = ???

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
          // TODO should load base URL from config
          //"url" -> s"https://first-responder-hack.herokuapp.com/webhooks/formstack/$hashtag",
          "url" -> "http://requestb.in/w88n43w8",
          "handshake_key" -> webhookKey
        )
      )
    )
  }

}
