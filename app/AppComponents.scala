import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider, EnvironmentVariableCredentialsProvider, AWSCredentialsProviderChain }
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3Client
import com.gu.googleauth.GoogleAuthConfig
import controllers.{ Webhooks, Auth, Application }
import mailgun.MailgunWebhookHandler
import models._
import org.joda.time.Duration
import play.api.ApplicationLoader.Context
import play.api.inject.{ NewInstanceInjector, SimpleInjector }
import play.api.libs.json.Json
import play.api.libs.ws.ning.NingWSComponents
import play.api.{ Logger, BuiltInComponentsFromContext }
import play.api.routing.Router
import router.Routes
import store.{ Dynamo, S3 }

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with NingWSComponents {

  def missingKey(description: String) = sys.error(s"$description missing. You can create an OAuth 2 client from the Credentials section of the Google dev console.")
  val googleAuthConfig = GoogleAuthConfig(
    clientId = configuration.getString("google.clientId") getOrElse missingKey("OAuth 2 client ID"),
    clientSecret = configuration.getString("google.clientSecret") getOrElse missingKey("OAuth 2 client secret"),
    redirectUrl = configuration.getString("google.redirectUrl") getOrElse missingKey("OAuth 2 callback URL"),
    domain = Some("guardian.co.uk"),
    maxAuthAge = Some(Duration.standardDays(90)),
    enforceValidity = true
  )
  val awsCreds = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new ProfileCredentialsProvider("capi"),
    new ProfileCredentialsProvider(),
    new InstanceProfileCredentialsProvider()
  )
  val s3BucketName = "first-responder-attachments-dev" // TODO read from conf
  val s3 = new S3(new AmazonS3Client(awsCreds), s3BucketName)

  val dynamoTableName = "first-responder-DEV" // TODO read from conf
  val dynamo = new Dynamo(new DynamoDB(new AmazonDynamoDBClient(awsCreds)), dynamoTableName)

  val mailgunApiKey = "foo" // TODO read from conf
  val mailgunWebhookHandler = new MailgunWebhookHandler(wsApi, s3, dynamo)

  val appController = new Application(googleAuthConfig)
  val authController = new Auth(googleAuthConfig, wsApi)
  val webhooksController = new Webhooks(mailgunApiKey, mailgunWebhookHandler)

  val assets = new controllers.Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, appController, webhooksController, authController, assets)

}
