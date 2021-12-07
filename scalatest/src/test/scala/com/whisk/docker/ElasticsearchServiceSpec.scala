package com.whisk.docker

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.time._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration

class ElasticsearchServiceSpec
    extends AnyFlatSpec
    with Matchers
    with DockerElasticsearchService
    with DockerTestKit
    with DockerKitDockerJava {

  implicit val pc: PatienceConfig = PatienceConfig(Span(20, Seconds), Span(1, Second))
  implicit val defaultOpsTimeout: Duration = Duration.Inf

  "elasticsearch container" should "be ready" in {
    isContainerReady(elasticsearchContainer).futureValue shouldBe true
    elasticsearchContainer.getPorts().futureValue.get(9300) should not be empty
    elasticsearchContainer.getIpAddresses().futureValue should not be (Seq.empty)
  }

}
