package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env

class ZookeeperServiceSpec(env: Env) extends Specification
    with DockerTestKit
    with DockerZookeeperService {

  implicit val ee = env.executionEnv

  def is = s2"""
  The Zookeeper container should be ready $x1
                                          """

  def x1 = isContainerReady(zookeeperContainer) must beTrue.await
}
