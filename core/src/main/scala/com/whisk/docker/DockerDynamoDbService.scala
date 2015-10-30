package com.whisk.docker

import scala.concurrent.duration._

trait DockerDynamoDbService extends DockerKit {

  val DefaultDynamoDbPort = 8000

  val dynamoContainer = DockerContainer("abcum/dynamodb")
    .withPorts(DefaultDynamoDbPort -> Some(DefaultDynamoDbPort))
    .withReadyChecker(
      DockerReadyChecker
        .LogLineContains("AbstractConnector:Started",true)
        .within(100.millis)
        .looped(20, 1250.millis)
    )

  abstract override def dockerContainers: List[DockerContainer] =
    dynamoContainer :: super.dockerContainers
}