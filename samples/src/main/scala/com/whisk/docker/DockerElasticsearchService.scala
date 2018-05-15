package com.whisk.docker

import scala.concurrent.duration._

trait DockerElasticsearchService extends DockerKit {
  override val StartContainersTimeout = 60.seconds

  val DefaultElasticsearchHttpPort = 9200
  val DefaultElasticsearchClientPort = 9300

  val elasticsearchContainer: DockerContainer = DockerContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4")
    .withPortMapping(
      DefaultElasticsearchHttpPort -> DockerPortMapping(Some(DefaultElasticsearchHttpPort)),
      DefaultElasticsearchClientPort -> DockerPortMapping(Some(DefaultElasticsearchClientPort)))
    .withEnv("discovery.type=single-node", "xpack.security.enabled=false")
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(DefaultElasticsearchHttpPort, "/", Some("0.0.0.0"))
        .within(100.millis)
        .looped(20, 1250.millis))

  abstract override def dockerContainers: List[DockerContainer] =
    elasticsearchContainer :: super.dockerContainers
}
