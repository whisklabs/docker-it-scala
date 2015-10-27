package com.whisk.docker

import scala.concurrent.duration._

trait DockerDynamoDbService extends DockerKit {

  val DefaultDynamoDbPort = 8000

  val dynamoContainer = DockerContainer("abcum/dynamodb")
    .withPorts(DefaultDynamoDbPort -> Some(DefaultDynamoDbPort))
    .withReadyChecker(
      DockerReadyChecker
        .Always
        .within(100.millis)
        .looped(20, 1250.millis)
    )

  abstract override def dockerContainers: List[DockerContainer] =
    dynamoContainer :: super.dockerContainers
}

/*
case class DynamoAlive(port: Int, host: Option[String] = Some("localhost")) extends DockerReadyChecker {

  lazy val iso8601 = new SimpleDateFormat("yyyyMMdd'T'HH:mm'Z'");

  override def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] = {
    container.getPorts().map(_(port)).flatMap { p =>
      val url = new URL("http", host.getOrElse(docker.host), p,
        "/?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAAXXXXXXXXXXXX%2F20150421%2Feu-west-1%2Fdynamodb%2Faws4_request&X-Amz-Date=20150421T075501Z&X-Amz-Expires=300&X-Amz-SignedHeaders=host%3Bx-amz-target&X-Amz-Signature=xxxxxxxxxxxxxxxxxxxxxxx HTTP/1.1.")
      Future {
        val con = url.openConnection().asInstanceOf[HttpURLConnection]
        try {
          con.setRequestMethod("POST")
          con.setRequestProperty("host",s"$host:$port")
          con.setRequestProperty("Authorization","AWS4-HMAC-SHA256 Credential=AKIAAXXXXXXXXXXXX/20150421/eu-west-1/dynamodb/aws4_request,SignedHeaders=host;x-amz-date;x-amz-target,Signature=")
          con.setRequestProperty("x-amz-date", iso8601.format(new java.util.Date()))
          con.setRequestProperty("content-type","application/x-amz-json-1.0")
          con.setRequestProperty("Content-Length","21")
          con.setRequestProperty("X-Amz-Content-Sha256","ab92c2bbb935dcf9db6b76ea026a523873c16015c2a74250e9ed26b8c4c8b438")

          con.getResponseCode == code
        } catch {
          case e: java.net.ConnectException =>
            false
        }
      }
    }
  }
}*/