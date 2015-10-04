package com.whisk.docker.config

import com.whisk.docker.DockerContainer

trait DockerPostgresService extends DockerKitConfig {

  val postgresContainer =
    configureDockerContainer("docker.postgres")

  abstract override def dockerContainers: List[DockerContainer] =
    postgresContainer :: super.dockerContainers
}
