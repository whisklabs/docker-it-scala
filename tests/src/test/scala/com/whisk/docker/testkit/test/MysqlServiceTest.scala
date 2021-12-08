package com.whisk.docker.testkit.test

import com.whisk.docker.testkit.{ContainerState, DockerMysqlService}
import org.scalatest.funsuite.AnyFunSuite

class MysqlServiceTest extends AnyFunSuite with DockerMysqlService {

  test("test container started") {
    assert(mysqlContainer.state().isInstanceOf[ContainerState.Ready], "mysql is ready")
    assert(mysqlContainer.mappedPortOpt(mysqlContainer.AdvertisedPort).nonEmpty,
           "mysql port exposed")
  }
}
