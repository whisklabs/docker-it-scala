package com.whisk.docker

import org.scalatest.Suite

import scala.concurrent.duration._

trait DockerMysqlService extends DockerTestKitForAll { self: Suite =>

  def MysqlAdvertisedPort = 3306
  val MysqlUser = "test"
  val MysqlPassword = "test"
  val MysqlDatabase = "test"

  val mysqlContainer = ContainerSpec("quay.io/whisk/fastboot-mysql:5.7.19")
    .withExposedPorts(MysqlAdvertisedPort)
    .withReadyChecker(
      DockerReadyChecker
        .Jdbc(
          driverClass = "com.mysql.jdbc.Driver",
          urlFunc = port => s"jdbc:mysql://${dockerClient.getHost}:$port/test",
          user = MysqlUser,
          password = MysqlPassword
        )
        .looped(25, 1.second)
    )
    .toContainer

  override val managedContainers: ManagedContainers = mysqlContainer.toManagedContainer
}
