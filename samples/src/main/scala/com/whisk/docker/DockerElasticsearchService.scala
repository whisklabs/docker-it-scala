package com.whisk.docker

import scala.concurrent.duration._

trait DockerElasticsearchService extends DockerKit {

  val DefaultElasticsearchHttpPort = 9200
  val DefaultElasticsearchClientPort = 9300

  val elasticsearchContainer = DockerContainer("elasticsearch:1.7.1")
    .withPorts(DefaultElasticsearchHttpPort -> None, DefaultElasticsearchClientPort -> None)
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(DefaultElasticsearchHttpPort, "/")
        .within(100.millis)
        .looped(20, 1250.millis)
    )

  abstract override def dockerContainers: List[DockerContainer] =
    elasticsearchContainer :: super.dockerContainers
}
