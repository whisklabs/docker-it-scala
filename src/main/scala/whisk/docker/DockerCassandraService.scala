package whisk.docker

trait DockerCassandraService extends DockerKit {
  self: DockerConfig =>

  val cassandraContainer = DockerContainer("")

  abstract override def dockerContainers = cassandraContainer :: super.dockerContainers
}
