package com.whisk.docker.config

import com.whisk.docker.DockerContainer

trait DockerCassandraService extends DockerKitConfig {

  val cassandraContainer = configureDockerContainer("docker.cassandra")

  abstract override def dockerContainers =
    cassandraContainer :: super.dockerContainers
}
