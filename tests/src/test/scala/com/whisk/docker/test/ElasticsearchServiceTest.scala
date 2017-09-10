package com.whisk.docker.test

import com.whisk.docker.{ContainerState, DockerElasticsearchService}
import org.scalatest.FunSuite

class ElasticsearchServiceTest extends FunSuite with DockerElasticsearchService {

  test("test container started") {
    assert(elasticsearchContainer.state().isInstanceOf[ContainerState.Ready],
           "elasticsearch container is ready")
    assert(elasticsearchContainer.mappedPorts().get(9200).nonEmpty, "elasticsearch port is exposed")
  }
}
