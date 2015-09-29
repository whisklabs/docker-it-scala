package whisk.docker

trait DockerCassandraService extends DockerKit {

  val cassandraContainer = configureDockerContainer("docker.cassandra")

  abstract override def dockerContainers = cassandraContainer :: super.dockerContainers
}
