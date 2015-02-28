package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import whisk.docker.test.DockerTestKit

class ZookeeperServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerZookeeperService with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "zookeeper container" should "be ready" in {
    zookeeperContainer.isReady().futureValue shouldBe true
  }

}