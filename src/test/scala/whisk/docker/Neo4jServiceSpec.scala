package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class Neo4jServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerTestKit
    with DockerClientConfig
    with DockerNeo4jService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "neo4j container" should "be ready" in {
    neo4jContainer.isReady().futureValue shouldBe true
  }

}
