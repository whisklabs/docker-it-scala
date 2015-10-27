package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env

class GitServiceSpec(env: Env) extends Specification
    with DockerGitService
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is = s2"""
  The Git Daemon container should be ready $x1
                                              """

  def x1 = gitContainer.isReady() must beTrue.await
}
