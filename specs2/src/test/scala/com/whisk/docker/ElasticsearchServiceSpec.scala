package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._

class ElasticsearchServiceSpec(env: Env)
    extends Specification
    with DockerTestKitDockerJava
    with DockerElasticsearchService
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is =
    s2"""
  The elasticsearch container should be ready $x1
                                              """

  def x1 = isContainerReady(elasticsearchContainer) must beTrue.await
}
