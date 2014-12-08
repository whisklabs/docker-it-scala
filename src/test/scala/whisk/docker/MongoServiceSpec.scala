package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import whisk.docker.test.DockerTestKit

class MongoServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerTestKit
    with DockerMongoService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "mongo container" should "be ready" in {
    mongoContainer.isReady().futureValue shouldBe true
  }

}
