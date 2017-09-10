package com.whisk.docker

import org.scalatest.Suite

trait DockerCassandraService extends DockerTestKitForAll { self: Suite =>

  val DefaultCqlPort = 9042

  val cassandraContainer = ContainerSpec("whisk/cassandra:2.1.8")
    .withExposedPorts(DefaultCqlPort)
    .withReadyChecker(DockerReadyChecker.LogLineContains("Starting listening for CQL clients on"))
    .toContainer

  override val managedContainers = cassandraContainer.toManagedContainer
}
