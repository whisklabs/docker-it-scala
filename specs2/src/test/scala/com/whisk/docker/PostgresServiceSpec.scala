package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.matcher.ResultMatchers
import org.specs2.specification.ExecutionEnvironment
import org.specs2.specification.core.Env

import scala.concurrent._

class PostgresServiceSpec(env: Env) extends Specification
    with DockerTestKit
    with DockerPostgresService {

  implicit val ee = env.executionEnv

  def is = s2"""
  The Postgres node should be ready with log line checker  $x1
                                                           """

  def x1 = isContainerReady(postgresContainer) must beTrue.await
}
