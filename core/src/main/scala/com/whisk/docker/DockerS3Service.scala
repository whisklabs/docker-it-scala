package com.whisk.docker

import java.net.{HttpURLConnection, URL}
import java.text.SimpleDateFormat

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._

trait DockerS3Service extends DockerKit {

  val DefaultS3Port = 9444

  val s3Container = DockerContainer("66pix/s3ninja")
    .withPorts(DefaultS3Port -> Some(DefaultS3Port))
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(DefaultS3Port, "/")
        .within(100.millis)
        .looped(20, 1250.millis)
    )

  abstract override def dockerContainers: List[DockerContainer] =
    s3Container :: super.dockerContainers
}