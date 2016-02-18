package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class Neo4jServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerTestKit with DockerNeo4jService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "neo4j container" should "be ready" in {
    neo4jContainer.isReady.futureValue shouldBe true
  }

  "neo4j container" should "pass ready checker with logs" in {
    val c = DockerReadyChecker.LogLineContains("Starting HTTP on port :7474")

    c(neo4jContainer).futureValue shouldBe true
  }

}
