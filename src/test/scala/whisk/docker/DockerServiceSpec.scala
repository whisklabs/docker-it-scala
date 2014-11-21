package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import scala.collection.JavaConversions._

class DockerServiceSpec extends FeatureSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerTestKit
    with DockerClientKit
    with PingContainerKit {

  implicit val pc = PatienceConfig(Span(10, Minutes), Span(1, Minute))

  feature("docker service") {
    scenario("docker service builder created") {
      dockerClient.infoCmd().exec().toString.contains("docker") shouldBe true

    }

    scenario("ping container initialized and then stopped") {
      whenReady(docker(pingContainer).id) { id =>
        println(id)
        dockerClient.listContainersCmd().exec().exists(_.getId == id) shouldBe true
        whenReady(docker(pingContainer).stop()) { id2 =>
          id2 shouldBe id

          dockerClient.listContainersCmd().exec().exists(_.getId == id) shouldBe false
        }

      }

    }
  }

}
