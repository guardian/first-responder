package store

import java.util

import com.amazonaws.services.dynamodbv2.document.spec.{ ScanSpec, QuerySpec }
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.document.{ AttributeUpdate, PrimaryKey, Item, DynamoDB }
import com.amazonaws.services.dynamodbv2.model._
import models._
import org.joda.time.{ DateTimeZone, DateTime }
import play.api.Logger
import play.api.libs.json.Json
import scala.collection.JavaConverters._

class Dynamo(db: DynamoDB, contributionsTableName: String, calloutsTableName: String) {

  import Dynamo._

  initTables()

  private val contributions = db.getTable(contributionsTableName)
  private val callouts = db.getTable(calloutsTableName)

  def save(callout: Callout): Unit = {
    val item = serialize(callout)
    callouts.putItem(item)
  }

  def save(contribution: Contribution): Unit = {
    val item = serialize(contribution)
    contributions.putItem(item)
  }

  def findCallouts(): Seq[Callout] = {
    val it = callouts.scan(new ScanSpec()).iterator().asScala
    it.map(deserialize[Callout]).toSeq.sortBy(_.createdAt.getMillis).reverse
  }

  def findCalloutByHashtag(hashtag: String): Option[Callout] = {
    val query = new QuerySpec()
      .withHashKey("hashtag", hashtag)
    val it = callouts.query(query).iterator()
    if (it.hasNext)
      Some(deserialize[Callout](it.next()))
    else
      None
  }

  def findContribution(hashtag: String, id: String): Option[Contribution] = {
    val query = new QuerySpec()
      .withHashKey("hashtag", hashtag)
      .withFilterExpression("id = :v_id")
      .withValueMap(new ValueMap().withString(":v_id", id))
    val it = contributions.query(query).iterator()
    if (it.hasNext)
      Some(deserialize[Contribution](it.next()))
    else
      None
  }

  def findContributionsByHashtagAndStatus(hashtag: String, status: ModerationStatus): Seq[Contribution] = {
    val query = new QuerySpec()
      .withHashKey("hashtag", hashtag)
      .withFilterExpression("moderationStatus = :s")
      .withValueMap(new ValueMap().withString(":s", status.entryName))
      .withScanIndexForward(false) // order by increasing age
    val it = contributions.query(query).iterator().asScala
    it.map(deserialize[Contribution]).toSeq
  }

  /**
   * Find the next contribution older than the given one, with the same hashtag and moderation status
   */
  def findNextContributionOlderThan(contribution: Contribution): Option[Contribution] = {
    val query = new QuerySpec()
      .withKeyConditionExpression("hashtag = :h and createdAt < :c")
      .withFilterExpression("moderationStatus = :s")
      .withValueMap(new ValueMap()
        .withString(":h", contribution.hashtag)
        .withString(":c", contribution.createdAt.withZone(DateTimeZone.UTC).toString)
        .withString(":s", contribution.moderationStatus.entryName))
      .withScanIndexForward(false) // order by increasing age
      .withMaxResultSize(1)
    val it = contributions.query(query).iterator().asScala
    if (it.hasNext)
      Some(deserialize[Contribution](it.next()))
    else
      None
  }

  /**
   * Find the latest 100 JustIn contributions across all hashtags
   */
  def findLatestJustInContributions(limit: Int = 100): Seq[Contribution] = {
    val scan = new ScanSpec()
      .withFilterExpression("moderationStatus = :s")
      .withValueMap(new ValueMap().withString(":s", ModerationStatus.JustIn.entryName))
      .withMaxResultSize(limit)
    val it = contributions.scan(scan).iterator().asScala
    it.map(deserialize[Contribution]).toSeq.sortBy(_.createdAt.getMillis).reverse
  }

  def updateModerationStatus(contribution: Contribution, newStatus: ModerationStatus): Contribution = {
    contributions.updateItem(
      new PrimaryKey(
        "hashtag", contribution.hashtag,
        "createdAt", contribution.createdAt.withZone(DateTimeZone.UTC).toString),
      new AttributeUpdate("moderationStatus").put(newStatus.entryName)
    )
    contribution.copy(moderationStatus = newStatus)
  }

  private def serialize[T: DynamoCodec](t: T): Item = implicitly[DynamoCodec[T]].toItem(t)

  private def deserialize[T: DynamoCodec](item: Item): T = implicitly[DynamoCodec[T]].fromItem(item)

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

    if (!tables.exists(_.getTableName == calloutsTableName)) {
      Logger.info(s"Creating DynamoDB table: $calloutsTableName")
      db.createTable(
        calloutsTableName,
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

  implicit val calloutCodec: DynamoCodec[Callout] = new DynamoCodec[Callout] {
    override def toItem(callout: Callout): Item = {
      new Item()
        .withPrimaryKey("hashtag", callout.hashtag)
        .withString("createdAt", callout.createdAt.withZone(DateTimeZone.UTC).toString)
        .withOptString("description", callout.description)
        .withOptString("formstackId", callout.formstackId)
    }

    override def fromItem(item: Item): Callout = {
      Callout(
        hashtag = item.getString("hashtag"),
        createdAt = new DateTime(item.getString("createdAt")).withZone(DateTimeZone.UTC),
        description = Option(item.getString("description")),
        formstackId = Option(item.getString("formstackId"))
      )
    }
  }

  implicit val contributionCodec: DynamoCodec[Contribution] = new DynamoCodec[Contribution] {
    override def toItem(contribution: Contribution): Item = {
      val attachments: java.util.List[String] =
        contribution.attachments.map(a => Json.stringify(Json.toJson(a))).asJava
      new Item()
        .withPrimaryKey("hashtag", contribution.hashtag)
        .withString("id", contribution.id)
        .withMap("contributor", contributor2map(contribution.contributor))
        .withString("channel", contribution.channel.toString)
        .withString("createdAt", contribution.createdAt.withZone(DateTimeZone.UTC).toString)
        .withOptString("subject", contribution.subject)
        .withString("body", contribution.body)
        .withList("attachments", attachments)
        .withString("moderationStatus", contribution.moderationStatus.entryName)
    }

    override def fromItem(item: Item): Contribution = {
      val attachments = item.getList[String]("attachments").asScala.map(j => Json.parse(j).as[Attachment])
      Contribution(
        hashtag = item.getString("hashtag"),
        id = item.getString("id"),
        contributor = Option(item.getMap[String]("contributor")).map(map2contributor).getOrElse(Contributor.Anonymous),
        channel = Channel.withName(item.getString("channel")),
        createdAt = new DateTime(item.getString("createdAt")).withZone(DateTimeZone.UTC),
        subject = Option(item.getString("subject")),
        body = item.getString("body"),
        attachments = attachments,
        moderationStatus = Option(item.getString("moderationStatus")).map(ModerationStatus.withName).getOrElse(ModerationStatus.JustIn)
      )
    }

    private def map2contributor(map: util.Map[String, String]): Contributor =
      Contributor(email = Option(map.get("email")), phone = Option(map.get("phone")))

    private def contributor2map(contributor: Contributor): util.Map[String, String] = {
      val hashmap = new util.HashMap[String, String]()
      contributor.email.foreach(e => hashmap.put("email", e))
      contributor.phone.foreach(p => hashmap.put("phone", p))
      hashmap
    }
  }

}

trait DynamoCodec[T] {
  def toItem(t: T): Item
  def fromItem(item: Item): T
}
