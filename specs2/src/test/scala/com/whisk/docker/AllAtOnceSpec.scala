package com.whisk.docker

import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._
import scala.concurrent.duration._

class AllAtOnceSpec(env: Env) extends Specification
    with DockerElasticsearchService
    with DockerCassandraService
    with DockerNeo4jService
    with DockerMongodbService
    with PingContainerKit {

  implicit val ee = env.executionEnv
  implicit val ec = env.executionContext

  def is = s2"""
  The all containers should be ready at the same time $x1
                                                      """
  def x1 = {
    dockerContainers.map(_.image).foreach(println)
    Future.sequence(dockerContainers.map(_.isReady)) must contain(beTrue).forall.await
  }
}
