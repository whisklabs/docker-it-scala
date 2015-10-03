package whisk.docker.config

import org.specs2._
import org.specs2.specification.core.Env

import scala.concurrent._

import whisk.docker.DockerTestKit

class CassandraServiceSpec(env: Env) extends Specification
    with DockerCassandraService
    with DockerTestKit {

  implicit val ee = env.executionEnv

  def is = s2"""
  The cassandra node should be ready with log line checker $x1
                                                           """

  def x1 = cassandraContainer.isReady() must beTrue.await
}
