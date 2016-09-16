package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env

class KafkaServiceSpec(env: Env)
    extends Specification
    with DockerTestKitDockerJava
    with DockerKafkaService
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is =
    s2"""
  The Kafka container should be ready $x1
                                      """

  def x1 = isContainerReady(kafkaContainer) must beTrue.await
}
