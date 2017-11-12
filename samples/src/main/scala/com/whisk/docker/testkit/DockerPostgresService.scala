package com.whisk.docker.testkit

import com.spotify.docker.client.messages.PortBinding
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerPostgresService extends DockerTestKitForAll { self: Suite =>

  def PostgresAdvertisedPort = 5432
  def PostgresExposedPort = 44444
  val PostgresUser = "nph"
  val PostgresPassword = "suitup"

  val postgresContainer = ContainerSpec("postgres:9.6.5")
    .withPortBindings((PostgresAdvertisedPort, PortBinding.of("0.0.0.0", PostgresExposedPort)))
    .withEnv(s"POSTGRES_USER=$PostgresUser", s"POSTGRES_PASSWORD=$PostgresPassword")
    .withReadyChecker(
      DockerReadyChecker
        .Jdbc(
          driverClass = "org.postgresql.Driver",
          user = PostgresUser,
          password = Some(PostgresPassword)
        )
        .looped(15, 1.second)
    )
    .toContainer

  override val managedContainers: ManagedContainers = postgresContainer.toManagedContainer
}
