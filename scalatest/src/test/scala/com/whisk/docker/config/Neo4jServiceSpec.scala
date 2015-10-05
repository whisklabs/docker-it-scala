package com.whisk.docker.config

import com.whisk.docker.DockerReadyChecker
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class Neo4jServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerTestKit with DockerNeo4jService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "neo4j container" should "be ready" in {
    neo4jContainer.isReady().futureValue shouldBe true
  }

  "neo4j container" should "show its logs" in {
    val is = docker.client.logContainerCmd(neo4jContainer.id.futureValue).withStdOut().exec()

    def pullLines(it: Iterator[String], num: Int): List[String] = num match {
      case 0 => Nil
      case _ if !it.hasNext => Nil
      case n =>
        it.next() :: pullLines(it, n - 1)
    }

    val src = scala.io.Source.fromInputStream(is)(scala.io.Codec.ISO8859)

    val lns = pullLines(src.getLines(), 10)

    println(lns)

    lns.size shouldBe 10
  }

  "neo4j container" should "pass ready checker with logs" in {
    val c = DockerReadyChecker.LogLineContains("Starting HTTP on port :7474")

    c(neo4jContainer).futureValue shouldBe true
  }

}
