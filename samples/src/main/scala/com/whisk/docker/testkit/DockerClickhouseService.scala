package com.whisk.docker.testkit

import com.spotify.docker.client.messages.PortBinding
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerClickhouseService extends DockerTestKitForAll { self: Suite =>
  override val dockerTestTimeouts: DockerTestTimeouts = DockerTestTimeouts(pull = 10.minutes, init = 10.minutes, stop = 1.minutes)

  def ClickhouseAdvertisedPort = 8123
  def ClickhouseExposedPort = 8123

  val ClickhouseUser = "default"
  val ClickhousePassword = ""

  val clickhouseContainer = ContainerSpec("clickhouse/clickhouse-server:23.6")
    .withEnv(s"CLICKHOUSE_USER=$ClickhouseUser", s"CLICKHOUSE_PASSWORD=$ClickhousePassword")
    .withPortBindings((ClickhouseAdvertisedPort, PortBinding.of("0.0.0.0", ClickhouseExposedPort)))
    .withReadyChecker(
      DockerReadyChecker
       .Jdbc(
         driverClass = "com.clickhouse.jdbc.ClickHouseDriver",
        user = ClickhouseUser,
        password = Some(ClickhousePassword)
      )
        .looped(15, 1.second)
   )
   .toContainer

  override val managedContainers: ManagedContainers = clickhouseContainer.toManagedContainer
}
