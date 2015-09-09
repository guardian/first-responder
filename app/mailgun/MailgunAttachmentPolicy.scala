package mailgun

import mailgun.MailgunWebhookParser.AttachmentInfo

object MailgunAttachmentPolicy {

  def accept(attachment: AttachmentInfo): Boolean = {
    validUrl(attachment.url) && validContentType(attachment.`content-type`) && notTooBig(attachment.size)

  }

  private val OneHundredMB = 100 * 1024 * 1024

  private def validUrl(url: String) = url.startsWith("https://api.mailgun.net/")
  private def validContentType(contentType: String) = contentType.startsWith("image/") || contentType.startsWith("video/")
  private def notTooBig(sizeInBytes: Int) = sizeInBytes < OneHundredMB

}
