import sbt._
import Keys._
import play.sbt._
import play.sbt.Play.autoImport._
import play.sbt.routes.RoutesKeys._
import com.typesafe.sbt.SbtScalariform._
import com.typesafe.sbt.packager.Keys._
import com.gu.riffraff.artifact._
import RiffRaffArtifact.autoImport._

object FirstResponderBuild extends Build {

  object Versions {
    val awsSdk = "1.10.16"
  }

  lazy val project = Project(id = "content-api-first-responder", base = file("."))
    .enablePlugins(PlayScala)
    .enablePlugins(RiffRaffArtifact)
    .settings(scalariformSettings)
    .settings(
      scalaVersion := "2.11.7",
      scalacOptions ++= Seq("-feature", "-deprecation"),
      resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
      resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        libraryDependencies ++= Seq(
        ws,
        "com.amazonaws" % "aws-java-sdk-dynamodb" % Versions.awsSdk,
        "com.amazonaws" % "aws-java-sdk-s3" % Versions.awsSdk,
        "com.beachape" %% "enumeratum-play" % "1.3.1",
        "com.gu" %% "play-googleauth" % "0.3.1",
        "com.twilio.sdk" % "twilio-java-sdk" % "4.5.0",
        "org.scalatestplus" %% "play" % "1.1.1" % "test",
        "com.adrianhurt" %% "play-bootstrap" % "1.0-P24-B3-SNAPSHOT"
      ),
      riffRaffPackageName := "content-api-first-responder",
      riffRaffManifestProjectName := "Content Platforms::first-responder",
      riffRaffPackageType := (packageZipTarball in config("universal")).value,
      riffRaffBuildIdentifier := sys.env.getOrElse("BUILD_NUMBER", "DEV"),
      riffRaffUploadArtifactBucket := Some("riffraff-artifact"),
      riffRaffUploadManifestBucket := Some("riffraff-builds"),
      routesGenerator := InjectedRoutesGenerator,
      routesImport += "models._"
    )

}
