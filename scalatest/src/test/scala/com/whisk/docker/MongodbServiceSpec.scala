package com.whisk.docker

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

class MongodbServiceSpec extends FlatSpec with Matchers
  with DockerTestKit
  with DockerMongodbService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  "mongodb node" should "be ready with log line checker" in {
    isContainerReady(mongodbContainer).futureValue shouldBe true
    mongodbContainer.getPorts().futureValue.get(27017) should not be empty
  }
}
