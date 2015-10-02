package whisk.docker.config

import whisk.docker.DockerContainer

trait DockerCassandraService extends DockerKitConfig {

  val cassandraContainer = configureDockerContainer("docker.cassandra")

  abstract override def dockerContainers =
    cassandraContainer :: super.dockerContainers
}
