package whisk.docker

trait DockerZookeeperService extends DockerKit {

  val zookeeperContainer = configureDockerContainer("docker.zookeeper")

  abstract override def dockerContainers: List[DockerContainer] =
    zookeeperContainer :: super.dockerContainers
}
