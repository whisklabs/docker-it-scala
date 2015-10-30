package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._

class DynamoDbServiceSpec(env: Env) extends Specification
    with DockerDynamoDbService
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is = s2"""
  The DynamoDb container should be ready $x1
                                              """

  def x1 = dynamoContainer.isReady() must beTrue.await
}
