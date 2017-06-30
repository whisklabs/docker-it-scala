package com.whisk.docker.config.test

import com.whisk.docker.{DockerContainer, DockerReadyChecker, VolumeMapping}
import com.whisk.docker.config.DockerKitConfig
import org.scalatest._

import scala.concurrent.duration._

class DockerConfigSpec extends FlatSpec with Matchers with DockerKitConfig {

  "Config-based configurations" should "produce same containers as code-based ones" in {
    val volumes = Seq(
      VolumeMapping(container = "/opt/data", host = "/opt/docker/data", rw = false),
      VolumeMapping(container = "/opt/log", host = "/opt/docker/log", rw = true)
    )

    val cassandraExpected = DockerContainer("whisk/cassandra:2.1.8")
      .withPorts(9042 -> None)
      .withReadyChecker(
        DockerReadyChecker.LogLineContains("Starting listening for CQL clients on"))
      .withVolumes(volumes)

    configureDockerContainer("docker.cassandra") shouldBe cassandraExpected

    val postgresExpected = DockerContainer("postgres:9.4.4")
      .withPorts((5432, None))
      .withEnv(s"POSTGRES_USER=nph", s"POSTGRES_PASSWORD=suitup")
      .withReadyChecker(
        DockerReadyChecker.LogLineContains("database system is ready to accept connections"))

    configureDockerContainer("docker.postgres") shouldBe postgresExpected

    val mongodbExpected = DockerContainer("mongo:3.0.6")
      .withPorts(27017 -> None)
      .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
      .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

    configureDockerContainer("docker.mongodb") shouldBe mongodbExpected

    val elasticExpected = DockerContainer("elasticsearch:1.7.1")
      .withEntrypoint("my", "custom", "entrypoint")
      .withPorts(9200 -> None, 9300 -> None)
      .withReadyChecker(
        DockerReadyChecker
          .HttpResponseCode(9200, "/")
          .within(100.millis)
          .looped(20, 1250.millis))

    configureDockerContainer("docker.elasticsearch") shouldBe elasticExpected
  }
}
