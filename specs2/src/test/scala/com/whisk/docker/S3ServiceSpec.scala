package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._

class S3ServiceSpec(env: Env) extends Specification
    with DockerS3Service
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is = s2"""
  The s3 container should be ready $x1
                                              """

  def x1 = s3Container.isReady() must beTrue.await
}
