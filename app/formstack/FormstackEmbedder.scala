package formstack

import play.api.libs.ws.WSAPI
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FormstackEmbedder(ws: WSAPI, formstackOAuthToken: String) {

  def getEmbedCode(formId: String): Future[Html] = {
    ws.url(s"https://www.formstack.com/api/v2/form/$formId.json?oauth_token=$formstackOAuthToken")
      .withHeaders("Content-Type" -> "application/json")
      .get()
      .map { response =>
        Html((response.json \ "javascript").as[String])
      }
  }

}
