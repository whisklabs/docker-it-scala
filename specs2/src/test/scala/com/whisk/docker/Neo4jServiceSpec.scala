package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._
import scala.concurrent.duration._

class Neo4jServiceSpec(env: Env) extends Specification
    with DockerTestKit
    with DockerNeo4jService {

  implicit val ee = env.executionEnv
  implicit val ec = env.executionContext

  def is = s2"""
  The neo4j container should
    be ready                     $x1
    pass ready checker with logs $x2
                                 """

  def x1 = neo4jContainer.isReady() must beTrue.await

  def x2 = {
    val c = DockerReadyChecker.LogLineContains("Starting HTTP on port :7474")
    c(neo4jContainer) must beTrue.await
  }
}
