package com.whisk.docker.config

import com.whisk.docker.DockerContainer

trait DockerNeo4jService extends DockerKitConfig {

  val neo4jContainer =
    configureDockerContainer("docker.neo4j")

  abstract override def dockerContainers: List[DockerContainer] =
    neo4jContainer :: super.dockerContainers
}
