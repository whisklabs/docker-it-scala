package com.whisk.docker

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.munit.DockerTestKit
import _root_.munit.FunSuite

class ElasticsearchServiceSpec
    extends FunSuite
    with DockerElasticsearchService
    with DockerTestKit
    with DockerKitDockerJava {

  test("elasticsearch container should be ready") {
    isContainerReady(elasticsearchContainer).map(assert(_))
    elasticsearchContainer.getPorts().map(m => assert(m.get(9300).nonEmpty))
    elasticsearchContainer.getIpAddresses().map(s => assert(s.nonEmpty))
  }

}
