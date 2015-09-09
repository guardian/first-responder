package store

import java.util

import com.amazonaws.services.dynamodbv2.document.{ Table, Item, DynamoDB }
import com.amazonaws.services.dynamodbv2.model._
import models.{ Channel, Attachment, Contributor, Contribution }
import org.joda.time.{ DateTimeZone, DateTime }
import play.api.Logger
import play.api.libs.json.Json
import scala.collection.JavaConverters._

class Dynamo(db: DynamoDB, contributionsTableName: String) {
  import Dynamo._

  initTables()

  private val contributions = db.getTable(contributionsTableName)

  def save(contribution: Contribution): Unit = {
    val item = serialize(contribution)
    contributions.putItem(item)
  }

  def findContributionsByHashtag(hashtag: String): Seq[Contribution] = {
    val it = contributions.query("hashtag", hashtag).iterator().asScala
    it.map(deserialize).toSeq
  }

  private def serialize(contribution: Contribution): Item = {
    val attachments: java.util.List[String] =
      contribution.attachments.map(a => Json.stringify(Json.toJson(a))).asJava
    new Item()
      .withPrimaryKey("hashtag", contribution.hashtag)
      .withString("id", contribution.id)
      .withOptString("contributor_email", contribution.contributor.email)
      .withString("channel", contribution.channel.toString)
      .withString("createdAt", contribution.createdAt.withZone(DateTimeZone.UTC).toString)
      .withOptString("subject", contribution.subject)
      .withString("body", contribution.body)
      .withList("attachments", attachments)
  }

  private def deserialize(item: Item): Contribution = {
    val attachments = item.getList[String]("attachments").asScala.map(j => Json.parse(j).as[Attachment])
    Contribution(
      hashtag = item.getString("hashtag"),
      id = item.getString("id"),
      contributor = Contributor(email = Option(item.getString("contributor_email"))),
      channel = Channel.withName(item.getString("channel")),
      createdAt = new DateTime(item.getString("createdAt")).withZone(DateTimeZone.UTC),
      subject = Option(item.getString("subject")),
      body = item.getString("body"),
      attachments = attachments
    )
  }

  private def initTables(): Unit = {
    val tables = db.listTables().iterator().asScala.toSeq
    if (!tables.exists(_.getTableName == contributionsTableName)) {
      Logger.info(s"Creating DynamoDB table: $contributionsTableName")
      db.createTable(
        contributionsTableName,
        util.Arrays.asList(
          new KeySchemaElement().withAttributeName("hashtag").withKeyType(KeyType.HASH),
          new KeySchemaElement().withAttributeName("createdAt").withKeyType(KeyType.RANGE)
        ),
        util.Arrays.asList(
          new AttributeDefinition().withAttributeName("hashtag").withAttributeType(ScalarAttributeType.S),
          new AttributeDefinition().withAttributeName("createdAt").withAttributeType(ScalarAttributeType.S)
        ),
        new ProvisionedThroughput(1L, 1L)
      )
    }
  }
}

object Dynamo {

  implicit class RichItem(val item: Item) extends AnyVal {

    def withOptString(key: String, value: Option[String]) = {
      value.fold(item)(v => item.withString(key, v))
    }

  }

}
