package com.whisk.docker.test

import com.whisk.docker.{ContainerState, DockerPostgresService}
import org.scalatest.FunSuite

class PostgresServiceTest extends FunSuite with DockerPostgresService {

  test("test container started") {
    assert(postgresContainer.state().isInstanceOf[ContainerState.Ready], "postgres is ready")
    assert(
      postgresContainer.mappedPorts().get(PostgresAdvertisedPort) === Some(PostgresExposedPort),
      "postgres port exposed")
  }
}
