package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._

import org.specs2.concurrent.ExecutionEnv

class MongodbServiceSpec(env: Env)
    extends Specification
    with DockerTestKitDockerJava
    with DockerTestKit
    with DockerMongodbService {

  implicit val ee: ExecutionEnv = env.executionEnv

  def is = s2"""
  The mongodb container should be ready $x1
                                        """

  def x1 = isContainerReady(mongodbContainer) must beTrue.await
}
