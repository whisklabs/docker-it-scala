package com.whisk.docker.test

import com.whisk.docker.{ContainerState, DockerMysqlService}
import org.scalatest.FunSuite

class MysqlServiceTest extends FunSuite with DockerMysqlService {

  test("test container started") {
    assert(mysqlContainer.state().isInstanceOf[ContainerState.Ready], "mysql is ready")
    assert(mysqlContainer.mappedPorts().get(MysqlAdvertisedPort).nonEmpty, "mysql port exposed")
  }
}
