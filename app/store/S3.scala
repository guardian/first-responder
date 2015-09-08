package store

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ CannedAccessControlList, PutObjectRequest, ObjectMetadata }
import models.Attachment

import scala.concurrent.Future
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global // TODO proper execution context for S3 client

class S3(client: AmazonS3Client, bucketName: String) {

  def storeAttachment(in: InputStream, mimeType: String): Future[Attachment] = {
    val id = java.util.UUID.randomUUID().toString

    // TODO use a folder structure in S3? yyyy/mm/dd ?
    val s3Key = id
    val metadata = new ObjectMetadata()
    metadata.setContentType(mimeType)
    val putObjectRequest = new PutObjectRequest(bucketName, s3Key, in, metadata)
      .withCannedAcl(CannedAccessControlList.PublicRead)

    // why isn't there an AmazonS3AsyncClient?
    Future {
      blocking {
        client.putObject(putObjectRequest)
        val url = {
          val region = client.getRegion.toAWSRegion.getName
          s"https://s3-${region}.amazonaws.com/$bucketName/$s3Key"
        }
        Attachment(url, mimeType)
      }
    }
  }

}
