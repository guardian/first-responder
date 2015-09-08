package store

import com.amazonaws.services.dynamodbv2.document.{ Table, Item, DynamoDB }
import models.{ Attachment, Contributor, Contribution }
import play.api.libs.json.Json
import scala.collection.JavaConverters._

class Dynamo(db: DynamoDB, tableName: String) {
  import Dynamo._

  private val table = db.getTable(tableName)

  def save(contribution: Contribution): Unit = {
    val item = serialize(contribution)
    table.putItem(item)
  }

  def findContributionsByHashtag(hashtag: String): Seq[Contribution] = {
    val it = table.query("hashtag", hashtag).iterator().asScala
    it.map(deserialize).toSeq
  }

  private def serialize(contribution: Contribution): Item = {
    new Item()
      .withPrimaryKey("hashtag", contribution.hashtag)
      .withOptString("contributor_email", contribution.contributor.email)
      .withOptString("subject", contribution.subject)
      .withString("body", contribution.body)
      .withJSON("attachments", Json.stringify(Json.toJson(contribution.attachments)))
  }

  private def deserialize(item: Item): Contribution = {
    Contribution(
      id = item.getString("id"),
      hashtag = item.getString("hashtag"),
      contributor = Contributor(email = Option(item.getString("contributor_email"))),
      subject = Option(item.getString("subject")),
      body = item.getString("body"),
      attachments = Json.parse(item.getJSON("attachments")).as[Seq[Attachment]]
    )
  }

}

object Dynamo {

  implicit class RichItem(val item: Item) extends AnyVal {

    def withOptString(key: String, value: Option[String]) = {
      value.fold(item)(v => item.withString(key, v))
    }

  }

}
