package whisk.docker

trait DockerCassandraService extends DockerKit {
  self: DockerConfig =>

  val cassandraContainer = DockerContainer("spotify/cassandra")
    .withPorts(9042 -> None)
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("Starting listening for CQL clients on")))

  abstract override def dockerContainers = cassandraContainer :: super.dockerContainers
}
