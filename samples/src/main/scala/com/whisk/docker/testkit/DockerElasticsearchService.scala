package com.whisk.docker.testkit

import java.util.UUID

import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerElasticsearchService extends DockerTestKitForAll {
  self: Suite =>

  val DefaultElasticsearchHttpPort = 9200
  val DefaultElasticsearchClientPort = 9300
  val EsClusterName = UUID.randomUUID().toString

  protected val elasticsearchContainer =
    ContainerSpec("docker.elastic.co/elasticsearch/elasticsearch:6.2.4")
      .withExposedPorts(DefaultElasticsearchHttpPort, DefaultElasticsearchClientPort)
      .withEnv(
        "http.host=0.0.0.0",
        "xpack.security.enabled=false",
        "http.cors.enabled: true",
        "http.cors.allow-origin: \"*\"",
        s"cluster.name=$EsClusterName",
        "discovery.type=single-node",
        "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      )
      .withReadyChecker(
        DockerReadyChecker
          .HttpResponseCode(DefaultElasticsearchHttpPort, "/")
          .within(100.millis)
          .looped(20, 1250.millis))
      .toContainer

  override val managedContainers: ManagedContainers = elasticsearchContainer.toManagedContainer
}
