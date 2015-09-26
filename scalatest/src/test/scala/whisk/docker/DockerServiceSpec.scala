package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class DockerServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with PingContainerKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "docker client" should "connect to docker" in {
    docker.client.infoCmd().exec().toString.contains("docker") shouldBe true
  }

  "docker adapter" should "create container" in {
    When("docker tries to get container's id")

    val id = pingContainer.id.futureValue
    id should not be null

    Then("id is in list of running containers")

    pingContainer.isRunning().futureValue shouldBe true

    When("docker is trying to stop container")
    pingContainer.stop().futureValue

    Then("There's no such id in the list of running containers")
    pingContainer.isRunning().futureValue shouldBe false
  }

  "docker container" should "be available from port" in {
    When("we are building a container with bound port")
    val ports = pongContainer.getPorts().futureValue

    Then("port 80 should be on container")
    ports.get(80) should be('nonEmpty)
  }

}
