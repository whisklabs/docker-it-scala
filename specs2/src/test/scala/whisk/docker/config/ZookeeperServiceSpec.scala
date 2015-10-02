package whisk.docker.config

import org.specs2._
import org.specs2.specification.core.Env

import scala.concurrent._

import whisk.docker.DockerTestKit

class ZookeeperServiceSpec(env: Env) extends Specification
    with DockerTestKit
    with DockerZookeeperService {

  implicit val ee = env.executionEnv

  def is = s2"""
  The Zookeeper container should be ready $x1
                                          """

  def x1 = zookeeperContainer.isReady() must beTrue.await
}
