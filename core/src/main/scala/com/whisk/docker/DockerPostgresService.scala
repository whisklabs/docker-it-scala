package com.whisk.docker

trait DockerPostgresService extends DockerKit {

  def PostgresAdvertisedPort = 5432
  val PostgresUser = "nph"
  val PostgresPassword = "suitup"

  val postgresContainer = DockerContainer("postgres:9.4.4")
    .withPorts((PostgresAdvertisedPort, None))
    .withEnv(s"POSTGRES_USER=$PostgresUser", s"POSTGRES_PASSWORD=$PostgresPassword")
    .withReadyChecker(
      DockerReadyChecker
        .LogLineContains("database system is ready to accept connections")
    )

  abstract override def dockerContainers: List[DockerContainer] =
    postgresContainer :: super.dockerContainers
}
