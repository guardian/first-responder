import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider, EnvironmentVariableCredentialsProvider, AWSCredentialsProviderChain }
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3Client
import com.gu.googleauth.GoogleAuthConfig
import controllers.{ Api, Webhooks, Auth, Application }
import formstack.{ DummyFormCreator, FormstackEmbedder, FormstackWebhookHandler, FormstackFormCreator }
import mailgun.MailgunWebhookHandler
import org.joda.time.Duration
import play.api.ApplicationLoader.Context
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi, MessagesApi }
import play.api.libs.ws.ning.NingWSComponents
import play.api.{ Mode, BuiltInComponentsFromContext }
import play.api.routing.Router
import store.{ Dynamo, S3 }
import twilio.TwilioWebhookHandler
import router.Routes

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
    val contributionsTableName = configuration.getString("aws.dynamo.contributionsTableName") getOrElse "first-responder-DEV-contributions"
    val calloutsTableName = configuration.getString("aws.dynamo.calloutsTableName") getOrElse "first-responder-DEV-callouts"
    val client: AmazonDynamoDBClient = new AmazonDynamoDBClient(awsCreds).withRegion(awsRegion)
    new Dynamo(new DynamoDB(client), contributionsTableName, calloutsTableName)
  }

  val mailgunWebhookHandler = {
    /** The key that we need to make requests to Mailgun's API */
    val mailgunApiKey = mandatoryConfigString("mailgun.apiKey")
    new MailgunWebhookHandler(wsApi, mailgunApiKey, s3, dynamo)
  }
  val emailDomain = configuration.getString("emailDomain") getOrElse "No domain set up"

  val twilioWebhookHandler = new TwilioWebhookHandler(dynamo)
  val twilioPhoneNumber = configuration.getString("twilio.phoneNumber") getOrElse "No phone number"

  /** The key that we use to protect our webhook endpoints from unauthorised requests */
  val webhooksKey = mandatoryConfigString("webhooksKey")
  /** The key that we use to protect our API endpoints from unauthorised requests */
  val apiKey = mandatoryConfigString("apiKey")

  val formstackOauthToken = mandatoryConfigString("formstack.oauthToken")
  val formstackFormCreator = {
    if (environment.mode == Mode.Prod) {
      val baseUrl = mandatoryConfigString("baseUrl")
      new FormstackFormCreator(wsApi, webhooksKey, formstackOauthToken, baseUrl)
    } else
      new DummyFormCreator
  }
  val formstackWebhookHandler = new FormstackWebhookHandler(wsApi, dynamo)
  val formstackEmbedder = new FormstackEmbedder(wsApi, formstackOauthToken)

  val messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, new DefaultLangs(configuration))
  val appController = new Application(dynamo, formstackEmbedder, formstackFormCreator, messagesApi, googleAuthConfig, twilioPhoneNumber, emailDomain)
  val authController = new Auth(googleAuthConfig, wsApi)
  val webhooksController = new Webhooks(webhooksKey, mailgunWebhookHandler, twilioWebhookHandler, formstackWebhookHandler)
  val apiController = new Api(apiKey, dynamo, formstackFormCreator)

  val assets = new controllers.Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, appController, webhooksController, apiController, authController, assets)

}
