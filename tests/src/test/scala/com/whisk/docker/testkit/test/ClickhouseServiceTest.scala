package com.whisk.docker.testkit.test

import com.whisk.docker.testkit.{ContainerState, DockerClickhouseService}
import org.scalatest.funsuite.AnyFunSuite

class ClickhouseServiceTest extends AnyFunSuite with DockerClickhouseService {
  test("test container started") {
    assert(clickhouseContainer.state().isInstanceOf[ContainerState.Ready], "clickhouse is ready")
    assert(clickhouseContainer.mappedPortOpt(ClickhouseAdvertisedPort) === Some(ClickhouseExposedPort),
      "clickhouse port exposed")
  }
}
