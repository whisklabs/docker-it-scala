package whisk.docker

trait PingContainerKit extends DockerServiceSetting {
  self: DockerClientConfig =>

  val pingContainer = DockerContainerDescription("debian:stable")
    .withCommand("/bin/echo", "hello-world")

  abstract override def docker = super.docker + pingContainer
}
