package whisk.docker

import org.scalatest.Suite
import whisk.docker.test.DockerTestKit

trait PingContainerKit extends DockerTestKit {
  self: Suite =>

  val pingContainer = DockerContainer("dockerfile/nginx").withPorts(80 -> None)

  val pongContainer = DockerContainer("dockerfile/nginx")
    .withPorts(80 -> None)
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(port = 80))

  abstract override def dockerContainers = pongContainer :: pingContainer :: super.dockerContainers
}
