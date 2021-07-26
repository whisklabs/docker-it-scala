package com.whisk.docker

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.time._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KafkaServiceSpec
    extends AnyFlatSpec
    with Matchers
    with DockerKafkaService
    with DockerTestKit
    with DockerKitSpotify {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "kafka container" should "be ready" in {
    isContainerReady(kafkaContainer).futureValue shouldBe true
  }

}
