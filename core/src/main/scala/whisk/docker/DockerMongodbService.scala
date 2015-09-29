package whisk.docker

trait DockerMongodbService extends DockerKit {

  val mongodbContainer = configureDockerContainer("docker.mongodb")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}
