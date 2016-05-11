package com.whisk.docker

import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

class AllAtOnceSpec extends FlatSpec with Matchers
    with DockerElasticsearchService
    with DockerCassandraService
    with DockerNeo4jService
    with DockerMongodbService
    with PingContainerKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "all containers" should "be ready at the same time" in {
    dockerContainers.map(_.image).foreach(println)
    dockerContainers.forall(c => isContainerReady(c).futureValue) shouldBe true
  }
}
