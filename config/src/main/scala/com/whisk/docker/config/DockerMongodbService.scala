package com.whisk.docker.config

import com.whisk.docker.DockerContainer

trait DockerMongodbService extends DockerKitConfig {

  val mongodbContainer = configureDockerContainer("docker.mongodb")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}
