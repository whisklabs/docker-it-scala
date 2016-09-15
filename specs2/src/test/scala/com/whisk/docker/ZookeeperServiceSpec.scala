package com.whisk.docker

import org.specs2._
import org.specs2.specification.core.Env

class ZookeeperServiceSpec(env: Env)
    extends Specification
    with DockerTestKitDockerJava
    with DockerZookeeperService {

  implicit val ee = env.executionEnv

  def is = s2"""
  The Zookeeper container should be ready $x1
                                          """

  def x1 = isContainerReady(zookeeperContainer) must beTrue.await
}
