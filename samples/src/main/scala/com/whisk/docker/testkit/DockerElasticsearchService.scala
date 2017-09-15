package com.whisk.docker.testkit

import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerElasticsearchService extends DockerTestKitForAll {
  self: Suite =>

  val DefaultElasticsearchHttpPort = 9200
  val DefaultElasticsearchClientPort = 9300

  val elasticsearchContainer: Container = ContainerSpec("elasticsearch:1.7.1")
    .withExposedPorts(DefaultElasticsearchHttpPort, DefaultElasticsearchClientPort)
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(DefaultElasticsearchHttpPort, "/")
        .within(100.millis)
        .looped(20, 1250.millis)
    )
    .toContainer

  override val managedContainers: ManagedContainers = elasticsearchContainer.toManagedContainer
}
