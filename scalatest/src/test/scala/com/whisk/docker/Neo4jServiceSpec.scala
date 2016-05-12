package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class Neo4jServiceSpec extends FlatSpec with Matchers
    with DockerTestKit with DockerNeo4jService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "neo4j container" should "be ready" in {
    isContainerReady(neo4jContainer).futureValue shouldBe true
  }

}
