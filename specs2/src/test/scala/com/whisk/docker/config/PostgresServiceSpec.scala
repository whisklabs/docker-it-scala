package com.whisk.docker.config

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env

class PostgresServiceSpec(env: Env) extends Specification
    with DockerTestKit
    with DockerPostgresService {

  implicit val ee = env.executionEnv

  def is = s2"""
  The Postgres node should be ready with log line checker  $x1
                                                           """

  def x1 = postgresContainer.isReady() must beTrue.await
}
