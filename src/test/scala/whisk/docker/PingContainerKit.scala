package whisk.docker

import org.scalatest.Suite

trait PingContainerKit extends DockerTestKit {
  self: Suite with DockerClientConfig =>

  val pingContainer = DockerContainer("dockerfile/nginx")

  val pongContainer = DockerContainer("dockerfile/nginx")
    .withPorts(80 -> None)
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(port = 80))

  abstract override def dockerContainers = pingContainer :: pongContainer :: super.dockerContainers
}
