package com.whisk.docker

import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._
import scala.concurrent.duration._

class DockerServiceSpec(env: Env) extends Specification
    with PingContainerKit {

  implicit val ee = env.executionEnv
  implicit val ec = env.executionContext

  def is = s2"""
  The docker client should connect to docker         $x1
  The docker adapter should create container         $x2
  The docker container should be available form port $x3
                                                     """

  def x1 = docker.client.infoCmd().exec().toString.contains("docker") must beTrue
  def x2 = {
    pingContainer.id must not be empty.await
    pingContainer.isRunning() must beTrue.await
    Await.result(pingContainer.stop(), 1.seconds)
    pingContainer.isRunning() must beFalse.await
  }
  def x3 = pongContainer.getPorts() must be have key(80).await
}
