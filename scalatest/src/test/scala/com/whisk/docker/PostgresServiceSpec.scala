package com.whisk.docker

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

class PostgresServiceSpec extends FlatSpec with Matchers
    with DockerTestKit
    with DockerPostgresService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  "postgres node" should "be ready with log line checker" in {
    isContainerReady(postgresContainer).futureValue shouldBe true
  }
}
