package whisk.docker

trait PingContainerKit extends DockerTestKit {

  val pingContainer = DockerContainer("nginx:1.7.11")

  val pongContainer = DockerContainer("nginx:1.7.11")
    .withPorts(80 -> None)
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(port = 80, path = "/", host = None, code = 200))

  abstract override def dockerContainers = pingContainer :: pongContainer :: super.dockerContainers
}
