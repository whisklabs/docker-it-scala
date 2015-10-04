package com.whisk.docker.config

import com.whisk.docker.DockerContainer

trait DockerZookeeperService extends DockerKitConfig {

  val zookeeperContainer = configureDockerContainer("docker.zookeeper")

  abstract override def dockerContainers: List[DockerContainer] =
    zookeeperContainer :: super.dockerContainers
}
