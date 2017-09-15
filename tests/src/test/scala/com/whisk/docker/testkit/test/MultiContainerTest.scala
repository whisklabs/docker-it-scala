package com.whisk.docker.testkit.test

import com.whisk.docker.testkit._
import org.scalatest.FunSuite

class MultiContainerTest
    extends FunSuite
    with DockerElasticsearchService
    with DockerMongodbService {

  override val managedContainers = ContainerGroup.of(elasticsearchContainer, mongodbContainer)

  test("both containers should be ready") {
    assert(elasticsearchContainer.state().isInstanceOf[ContainerState.Ready],
           "elasticsearch container is ready")
    assert(elasticsearchContainer.mappedPorts().get(9200).nonEmpty, "elasticsearch port is exposed")

    assert(mongodbContainer.state().isInstanceOf[ContainerState.Ready], "mongodb is ready")
    assert(mongodbContainer.mappedPorts().get(27017).nonEmpty, "port 2017 is exposed")
  }
}
