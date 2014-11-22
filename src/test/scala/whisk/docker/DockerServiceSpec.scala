package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

class DockerServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerTestKit
    with DockerClientKit
    with PingContainerKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "docker client" should "connect to docker" in {
    dockerClient.infoCmd().exec().toString.contains("docker") shouldBe true
  }

  "docker adapter" should "create container" in {
    When("docker tries to get container's id")

    val id = Await.result(pingContainer.id, Duration("20 seconds"))

    Then("id is in list of running containers")
    dockerClient.listContainersCmd().exec().exists(_.getId == id) shouldBe true

    When("docker is trying to stop container")
    val id2 = pingContainer.stop().futureValue

    Then("id matches the created container's id")
    id2 shouldBe id

    And("There's no such id in the list of running containers")
    dockerClient.listContainersCmd().exec().exists(_.getId == id) shouldBe false
  }

}
