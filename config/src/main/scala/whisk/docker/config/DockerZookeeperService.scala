package whisk.docker.config

import whisk.docker.DockerContainer

trait DockerZookeeperService extends DockerKitConfig {

  val zookeeperContainer = configureDockerContainer("docker.zookeeper")

  abstract override def dockerContainers: List[DockerContainer] =
    zookeeperContainer :: super.dockerContainers
}
