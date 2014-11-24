package whisk.docker

import org.scalatest.Suite

trait PingContainerKit extends DockerTestKit {
  self: Suite with DockerClientConfig =>

  val pingContainer = DockerContainer("dockerfile/nginx")

  val pongContainer = DockerContainer("dockerfile/nginx").withPorts(80 -> None)

  abstract override def dockerContainers = pingContainer :: pongContainer :: super.dockerContainers
}
