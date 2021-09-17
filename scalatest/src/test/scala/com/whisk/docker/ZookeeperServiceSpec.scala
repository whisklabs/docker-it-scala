package com.whisk.docker

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.time._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ZookeeperServiceSpec
    extends AnyFlatSpec
    with Matchers
    with DockerZookeeperService
    with DockerTestKit
    with DockerKitDockerJava {

  implicit val pc: PatienceConfig = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "zookeeper container" should "be ready" in {
    isContainerReady(zookeeperContainer).futureValue shouldBe true
  }

}
