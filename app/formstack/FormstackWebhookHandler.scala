package formstack

import formstack.FormstackWebhookParser.Payload
import models._
import play.api.libs.ws.WSAPI
import store.Dynamo

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FormstackWebhookHandler(ws: WSAPI, dynamo: Dynamo) {

  def handlePayload(hashtag: String, payload: Payload): Future[Unit] = {
    val attachment: Future[Option[Attachment]] = {
      val option: Option[Future[Attachment]] = payload.attachmentUrl.map { url =>
        ws.url(url).head().map { resp =>
          val mimeType = resp.header("Content-Type").getOrElse("unknown")
          Attachment(url, mimeType)
        }
      }
      option match {
        case Some(fA) => fA.map(a => Some(a))
        case None => Future.successful(None)
      }
    }

    attachment map { a =>
      val contribution = Contribution(
        hashtag = hashtag,
        contributor = Contributor.Anonymous,
        channel = Channel.Form,
        subject = None,
        body = payload.body,
        attachments = a.toSeq
      )
      dynamo.save(contribution)
    }
  }

}
