package formstack

import formstack.FormstackWebhookParser.Payload
import models._
import play.api.libs.ws.WSAPI
import store.Dynamo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FormstackWebhookHandler(ws: WSAPI, dynamo: Dynamo) {

  def handlePayload(hashtag: String, payload: Payload): Future[Unit] = {
    val attachments: Future[Seq[Attachment]] = Future.traverse(payload.attachmentUrls) { url =>
      ws.url(url).head().map { resp =>
        val mimeType = resp.header("Content-Type").getOrElse("unknown")
        Attachment(url, mimeType)
      }
    }
    attachments map { a =>
      val contribution = Contribution(
        hashtag = hashtag,
        contributor = Contributor(email = payload.email, phone = None),
        channel = Channel.Form,
        subject = None,
        body = toBody(payload.textFields),
        attachments = a,
        notes = None
      )
      dynamo.save(contribution)
    }
  }

  private def toBody(fields: Map[String, String]): String = {
    fields.map {
      case ((k, v)) =>
        s"""
        |$k:
        |$v
        |
      """.stripMargin
    }.mkString
  }

}
