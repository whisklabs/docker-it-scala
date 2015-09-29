package whisk.docker

trait DockerPostgresService extends DockerKit {

  val postgresContainer =
    configureDockerContainer("docker.postgres")

  abstract override def dockerContainers: List[DockerContainer] =
    postgresContainer :: super.dockerContainers
}
