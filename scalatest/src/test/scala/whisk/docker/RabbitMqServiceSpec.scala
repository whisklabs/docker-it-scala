package whisk.docker

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}

class RabbitMqServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures
with DockerTestKit
with DockerRabbitMqService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "rabbit mq node" should "be ready with log line checker" in {
    rabbitMqContainer.isReady().futureValue shouldBe true
  }

}