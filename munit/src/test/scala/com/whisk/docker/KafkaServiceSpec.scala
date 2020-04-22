package com.whisk.docker

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.munit.DockerTestKit
import _root_.munit.FunSuite

class KafkaServiceSpec
    extends FunSuite
    with DockerKafkaService
    with DockerTestKit
    with DockerKitSpotify {

  test("kafka container should be ready") {
    isContainerReady(kafkaContainer).map(assert(_))
  }

}
