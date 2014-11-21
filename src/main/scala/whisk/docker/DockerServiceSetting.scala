package whisk.docker

trait DockerServiceSetting {
  self: DockerClientConfig =>
  def docker: DockerSetting = DockerSetting()
}
