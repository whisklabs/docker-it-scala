package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class ZookeeperServiceSpec extends FlatSpec with Matchers
    with DockerZookeeperService with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "zookeeper container" should "be ready" in {
    isContainerReady(zookeeperContainer).futureValue shouldBe true
  }

}
