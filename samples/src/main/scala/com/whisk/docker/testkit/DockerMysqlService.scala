package com.whisk.docker.testkit

import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

import scala.concurrent.duration._

class MysqlContainer(image: String) extends BaseContainer {

  val AdvertisedPort = 3306
  val User = "root"
  val Password = "test"
  val Database = "test"

  override val spec: ContainerSpec = {
    ContainerSpec(image)
      .withExposedPorts(AdvertisedPort)
      .withReadyChecker(
        DockerReadyChecker
          .Jdbc(
            driverClass = "com.mysql.jdbc.Driver",
            user = User,
            password = Password,
            database = Some(Database)
          )
          .looped(25, 1.second)
      )
  }
}

trait DockerMysqlService extends DockerTestKitForAll { self: Suite =>

  val mysqlContainer = new MysqlContainer("quay.io/whisk/fastboot-mysql:5.7.19")

  override val managedContainers: ManagedContainers = mysqlContainer.toManagedContainer
}
