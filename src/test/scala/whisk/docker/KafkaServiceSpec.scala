package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import whisk.docker.test.DockerTestKit

class KafkaServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerKafkaService with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "kafka container" should "be ready" in {
    kafkaContainer.isReady().futureValue shouldBe true
  }

}