package whisk.docker

import com.spotify.docker.client.DockerClient.LogsParameter
import com.spotify.docker.client.LogStream
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import whisk.docker.test.DockerTestKit

class Neo4jServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerTestKit
    with DockerNeo4jService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "neo4j container" should "be ready" in {
    neo4jContainer.isReady().futureValue shouldBe true
  }

  "neo4j container" should "show its logs" in {
    val is = docker.client.logs(neo4jContainer.id.futureValue, LogsParameter.STDOUT)

    def pullLines(it: LogStream, num: Int): List[String] = num match {
      case 0 => Nil
      case _ if !it.hasNext => Nil
      case n =>
        io.Source.fromBytes(it.next().content().array()).mkString :: pullLines(it, n - 1)
    }

    val lns = pullLines(is, 10)

    println(lns)

    lns.size shouldBe 10
  }

  "neo4j container" should "pass ready checker with logs" in {
    val c = DockerReadyChecker.LogLine(_.contains("Starting HTTP on port :7474"))

    c(neo4jContainer).futureValue shouldBe true
  }

}
