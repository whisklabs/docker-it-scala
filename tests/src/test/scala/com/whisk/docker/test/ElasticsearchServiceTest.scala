package com.whisk.docker.test

import com.whisk.docker.{ContainerState, DockerElasticsearchService}
import org.scalatest.FunSuite

class ElasticsearchServiceTest extends FunSuite with DockerElasticsearchService {

  test("test container started") {
    elasticsearchContainer.state().isInstanceOf[ContainerState.Ready]
  }
}
