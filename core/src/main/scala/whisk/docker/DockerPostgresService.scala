package whisk.docker

trait DockerPostgresService extends DockerKit {

  def DefaultPostgresPort = 5440
  def PostgresAdvertisedPort = 5432
  val PostgresUser = "aa-it-db"
  val PostgresPassword = "abovetopsecret"

  val postgresContainer = DockerContainer("postgres:9.4.4")
    .withPorts((PostgresAdvertisedPort, Some(DefaultPostgresPort)))
    .withEnv(s"POSTGRES_USER=$PostgresUser", s"POSTGRES_PASSWORD=$PostgresPassword")
    .withReadyChecker(
      DockerReadyChecker
        .LogLine(_.contains("database system is ready to accept connections"))
    )

  abstract override def dockerContainers: List[DockerContainer] = postgresContainer :: super.dockerContainers
}
