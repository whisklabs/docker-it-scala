package com.whisk.docker.testkit.test

import com.whisk.docker.testkit.{ContainerState, DockerPostgresService}
import org.scalatest.funsuite.AnyFunSuite

class PostgresServiceTest extends AnyFunSuite with DockerPostgresService {

  test("test container started") {
    assert(postgresContainer.state().isInstanceOf[ContainerState.Ready], "postgres is ready")
    assert(postgresContainer.mappedPortOpt(PostgresAdvertisedPort) === Some(PostgresExposedPort),
           "postgres port exposed")
  }
}
