import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider, EnvironmentVariableCredentialsProvider, AWSCredentialsProviderChain }
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3Client
import com.gu.googleauth.GoogleAuthConfig
import controllers.{ Webhooks, Auth, Application }
import mailgun.MailgunWebhookHandler
import org.joda.time.Duration
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ning.NingWSComponents
import play.api.BuiltInComponentsFromContext
import play.api.routing.Router
import router.Routes
import store.{ Dynamo, S3 }

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with NingWSComponents {

  def mandatoryConfigString(key: String): String =
    configuration.getString(key) getOrElse sys.error(s"Missing config key: $key")

  val googleAuthConfig = {
    def missingKey(description: String) =
      sys.error(s"$description missing. You can create an OAuth 2 client from the Credentials section of the Google dev console.")
    GoogleAuthConfig(
      clientId = configuration.getString("google.clientId") getOrElse missingKey("OAuth 2 client ID"),
      clientSecret = configuration.getString("google.clientSecret") getOrElse missingKey("OAuth 2 client secret"),
      redirectUrl = configuration.getString("google.redirectUrl") getOrElse missingKey("OAuth 2 callback URL"),
      domain = Some("guardian.co.uk"),
      maxAuthAge = Some(Duration.standardDays(90)),
      enforceValidity = true
    )
  }
  val awsCreds = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new ProfileCredentialsProvider("capi"),
    new ProfileCredentialsProvider(),
    new InstanceProfileCredentialsProvider()
  )
  val awsRegion = Regions.fromName(configuration.getString("aws.region") getOrElse "eu-west-1")

  val s3 = {
    val s3BucketName = configuration.getString("aws.s3.bucketName") getOrElse "first-responder-attachments-dev"
    val client: AmazonS3Client = new AmazonS3Client(awsCreds).withRegion(awsRegion)
    new S3(client, s3BucketName)
  }

  val dynamo = {
    val dynamoTableName = configuration.getString("aws.s3.dynamo.contributionsTableName") getOrElse "first-responder-DEV-contributions"
    val client: AmazonDynamoDBClient = new AmazonDynamoDBClient(awsCreds).withRegion(awsRegion)
    new Dynamo(new DynamoDB(client), dynamoTableName)
  }

  val mailgunWebhookHandler = {
    /** The key that we need to make requests to Mailgun's API */
    val mailgunApiKey = mandatoryConfigString("mailgun.apiKey")
    new MailgunWebhookHandler(wsApi, mailgunApiKey, s3, dynamo)
  }
  /** The key that we use to protect our webhook endpoint from unauthorised requests */
  val mailgunWebhookKey = mandatoryConfigString("mailgun.webhookKey")

  val appController = new Application(googleAuthConfig)
  val authController = new Auth(googleAuthConfig, wsApi)
  val webhooksController = new Webhooks(mailgunWebhookKey, mailgunWebhookHandler)

  val assets = new controllers.Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, appController, webhooksController, authController, assets)

}
