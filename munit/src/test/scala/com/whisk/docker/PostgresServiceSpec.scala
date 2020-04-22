package com.whisk.docker

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.munit.DockerTestKit
import _root_.munit.FunSuite

class PostgresServiceSpec
    extends FunSuite
    with DockerTestKit
    with DockerPostgresService {

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(
    DefaultDockerClient.fromEnv().build())

  test("postgres node should be ready with log line checker") {
    isContainerReady(postgresContainer).map(assert(_))
  }
}
