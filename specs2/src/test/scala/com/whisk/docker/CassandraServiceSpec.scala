package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env

class CassandraServiceSpec(env: Env) extends Specification
    with DockerCassandraService
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is = s2"""
  The cassandra node should be ready with log line checker $x1
                                                           """

  def x1 = cassandraContainer.isReady() must beTrue.await
}
