package whisk.docker

trait DockerCassandraService extends DockerKit {

  val DefaultCqlPort = 9042

  val cassandraContainer = DockerContainer("cassandra:2.1.5")
    .withPorts(DefaultCqlPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("Starting listening for CQL clients on")))

  abstract override def dockerContainers = cassandraContainer :: super.dockerContainers
}
