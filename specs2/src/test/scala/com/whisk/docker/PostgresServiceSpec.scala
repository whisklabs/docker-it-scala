package com.whisk.docker

import org.specs2._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.core.Env

class PostgresServiceSpec(env: Env)
    extends Specification
    with DockerTestKitDockerJava
    with DockerPostgresService {

  implicit val ee: ExecutionEnv = env.executionEnv

  def is = s2"""
  The Postgres node should be ready with log line checker  $x1
                                                           """

  def x1 = isContainerReady(postgresContainer) must beTrue.await
}
