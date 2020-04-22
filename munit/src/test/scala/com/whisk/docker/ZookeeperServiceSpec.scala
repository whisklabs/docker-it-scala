package com.whisk.docker

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.munit.DockerTestKit
import _root_.munit.FunSuite

class ZookeeperServiceSpec
    extends FunSuite
    with DockerZookeeperService
    with DockerTestKit
    with DockerKitDockerJava {

  test("zookeeper container should be ready") {
    isContainerReady(zookeeperContainer).map(assert(_))
  }

}
