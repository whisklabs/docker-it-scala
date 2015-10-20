package com.whisk.docker.config.test

import com.whisk.docker.{DockerReadyChecker, DockerContainer}
import com.whisk.docker.config.DockerKitConfig
import org.scalatest._

import scala.concurrent.duration._

class DockerConfigSpec extends FlatSpec with Matchers with DockerKitConfig {

  "Config-based configurations" should "produce same containers as code-based ones" in {
    val cassandraExpected =
      DockerContainer("whisk/cassandra:2.1.8")
        .withPorts(9042 -> None)
        .withReadyChecker(DockerReadyChecker.LogLineContains("Starting listening for CQL clients on"))

    configureDockerContainer("docker.cassandra") shouldBe cassandraExpected

    val postgresExpected =
      DockerContainer("postgres:9.4.4")
        .withPorts((5432, None))
        .withEnv(s"POSTGRES_USER=nph", s"POSTGRES_PASSWORD=suitup")
        .withReadyChecker(
          DockerReadyChecker
            .LogLineContains("database system is ready to accept connections"))

    configureDockerContainer("docker.postgres") shouldBe postgresExpected

    val mongodbExpected =
      DockerContainer("mongo:3.0.6")
        .withPorts(27017 -> None)
        .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
        .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

    configureDockerContainer("docker.mongodb") shouldBe mongodbExpected

    val redisExpected =
      DockerContainer("redis:3.0.5")
        .withPorts(6379 -> None)
        .withReadyChecker(DockerReadyChecker.LogLineContains("Server started"))

    configureDockerContainer("docker.redis") shouldBe redisExpected

    val redisSentinelExpected =
      DockerContainer("joshula/redis-sentinel")
        .withPorts(26379 -> None)
        .withReadyChecker(DockerReadyChecker.LogLineContains("monitor master mymaster"))
        .withCommand("--sentinel announce-ip localhost", "--sentinel announce-port 26379")

    configureDockerContainer("docker.redis-sentinel") shouldBe redisSentinelExpected

    val elasticExpected =
      DockerContainer("elasticsearch:1.7.1")
        .withPorts(9200 -> None, 9300 -> None)
        .withReadyChecker(
          DockerReadyChecker
            .HttpResponseCode(9200, "/")
            .within(100.millis)
            .looped(20, 1250.millis))

    configureDockerContainer("docker.elasticsearch") shouldBe elasticExpected
  }
}
