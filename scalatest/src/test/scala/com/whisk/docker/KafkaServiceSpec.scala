package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.time._

class KafkaServiceSpec extends FlatSpec with Matchers
    with DockerKafkaService with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "kafka container" should "be ready" in {
    kafkaContainer.isReady().futureValue shouldBe true
  }

}
