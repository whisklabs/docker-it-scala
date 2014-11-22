package whisk.docker

import org.scalatest.Suite

trait PingContainerKit extends DockerTestKit {
  self: Suite with DockerClientConfig =>

  val pingContainer = DockerContainer("dockerfile/nginx")

  abstract override def dockerContainers = pingContainer :: super.dockerContainers
}
