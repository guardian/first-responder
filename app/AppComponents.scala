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
    configuration.getString(key) getOrElse (sys.error(s"Missing config key: $key"))

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
  val awsRegion = Regions.fromName("eu-west-1") // TODO read from conf
  val s3BucketName = "first-responder-attachments-dev" // TODO read from conf
  val s3Client: AmazonS3Client = new AmazonS3Client(awsCreds).withRegion(awsRegion)
  val s3 = new S3(s3Client, s3BucketName)

  val dynamoTableName = "first-responder-DEV" // TODO read from conf
  val dynamoClient: AmazonDynamoDBClient = new AmazonDynamoDBClient(awsCreds).withRegion(awsRegion)
  val dynamo = new Dynamo(new DynamoDB(dynamoClient), dynamoTableName)

  val mailgunApiKey = mandatoryConfigString("mailgun.apiKey")
  /** The key that we use to protect our webhook endpoint from unauthorised requests */
  val mailgunWebhookKey = "foo" // TODO read from conf
  val mailgunWebhookHandler = new MailgunWebhookHandler(wsApi, mailgunApiKey, s3, dynamo)

  val appController = new Application(googleAuthConfig)
  val authController = new Auth(googleAuthConfig, wsApi)
  val webhooksController = new Webhooks(mailgunWebhookKey, mailgunWebhookHandler)

  val assets = new controllers.Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, appController, webhooksController, authController, assets)

}
