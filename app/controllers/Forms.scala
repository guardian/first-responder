package controllers

import play.api.data.Form
import play.api.data.Forms._

object Forms {

  case class CreateCalloutData(hashtag: String, description: Option[String])

  val createCalloutForm = Form(
    mapping(
      "hashtag" -> nonEmptyText(minLength = 2, maxLength = 45),
      "description" -> optional(text)
    )(CreateCalloutData.apply)(CreateCalloutData.unapply)
  )

}
