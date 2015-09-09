package mailgun

import java.io.ByteArrayInputStream

import mailgun.MailgunWebhookParser.{ AttachmentInfo, Payload }
import models._
import play.api.libs.ws.{ WSAuthScheme, WSAPI }
import store.{ Dynamo, S3 }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MailgunWebhookHandler(ws: WSAPI, mailgunApiKey: String, s3: S3, dynamo: Dynamo) {

  def handlePayload(payload: Payload): Future[Contribution] = {
    val fContribution = for {
      attachments <- copyAttachmentsToS3(payload.attachments)
    } yield {
      Contribution(
        hashtag = emailAddressToHashtag(payload.to),
        contributor = Contributor(email = Some(payload.from)),
        channel = Channel.Mail,
        subject = payload.subject,
        body = payload.body,
        attachments = attachments
      )
    }
    fContribution map { c =>
      dynamo.save(c)
      c
    }
  }

  /**
   * Emails sent to refugeecrisis@foo.com should be given the hashtag "refugeecrisis"
   */
  private def emailAddressToHashtag(email: String) = email.takeWhile(_ != '@')

  private def copyAttachmentsToS3(attachmentInfos: Seq[AttachmentInfo]): Future[Seq[Attachment]] = {
    Future.traverse(attachmentInfos) { ai =>
      ws.url(ai.url)
        .withAuth(username = "api", password = mailgunApiKey, scheme = WSAuthScheme.BASIC)
        .get().flatMap { resp =>
          // TODO could do some voodoo here to turn an Enumerator[Array[Byte]] into an InputStream for better memory perf
          val inputStream = new ByteArrayInputStream(resp.bodyAsBytes)
          s3.storeAttachment(inputStream, ai.`content-type`)
        }
    }
  }

}
