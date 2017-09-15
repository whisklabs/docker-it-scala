package com.whisk.docker.testkit.test

import com.whisk.docker.testkit.{ContainerState, DockerElasticsearchService}
import org.scalatest.FunSuite

class ElasticsearchServiceTest extends FunSuite with DockerElasticsearchService {

  test("test container started") {
    assert(elasticsearchContainer.state().isInstanceOf[ContainerState.Ready],
           "elasticsearch container is ready")
    assert(elasticsearchContainer.mappedPortOpt(9200).nonEmpty, "elasticsearch port is exposed")
  }
}
