package com.whisk.docker.testkit

import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

trait DockerMongodbService extends DockerTestKitForAll {
  self: Suite =>

  val DefaultMongodbPort = 27017

  val mongodbContainer = ContainerSpec("mongo:3.4.8")
    .withExposedPorts(DefaultMongodbPort)
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .toContainer

  override val managedContainers: ManagedContainers = mongodbContainer.toManagedContainer
}
