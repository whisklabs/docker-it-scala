package com.whisk.docker

import com.whisk.docker.impl.spotify.DockerKitSpotify
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class DependencyGraphReadyCheckSpec extends FlatSpec with Matchers with DockerKitSpotify {

  override val StartContainersTimeout = 45 seconds

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  val zookeeperContainer =
    DockerContainer("confluentinc/cp-zookeeper:3.1.2", name = Some("zookeeper"))
      .withEnv("ZOOKEEPER_TICK_TIME=2000", "ZOOKEEPER_CLIENT_PORT=2181")
      .withReadyChecker(DockerReadyChecker.LogLineContains("binding to port"))

  val kafkaContainer = DockerContainer("confluentinc/cp-kafka:3.1.2", name = Some("kafka"))
    .withEnv("KAFKA_BROKER_ID=1",
             "KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181",
             "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092")
    .withLinks(ContainerLink(zookeeperContainer, "zookeeper"))
    .withReadyChecker(DockerReadyChecker.LogLineContains("[Kafka Server 1], started"))

  val schemaRegistryContainer = DockerContainer("confluentinc/cp-schema-registry:3.1.2",
                                                name = Some("schema_registry"))
    .withEnv("SCHEMA_REGISTRY_HOST_NAME=schema_registry",
             "SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL=zookeeper:2181")
    .withLinks(ContainerLink(zookeeperContainer, "zookeeper"),
               ContainerLink(kafkaContainer, "kafka"))
    .withReadyChecker(DockerReadyChecker.LogLineContains("Session establishment complete on server"))

  override def dockerContainers =
    schemaRegistryContainer :: kafkaContainer :: zookeeperContainer :: super.dockerContainers

  "all containers except the leaves of the dependency graph" should "be ready after initialization" in {
    startAllOrFail()

    try {
      containerManager.isReady(zookeeperContainer).isCompleted shouldBe true
      containerManager.isReady(kafkaContainer).isCompleted shouldBe true
      containerManager.isReady(schemaRegistryContainer).isCompleted shouldBe false

      Await.ready(containerManager.isReady(schemaRegistryContainer), 45 seconds)

      containerManager.isReady(schemaRegistryContainer).isCompleted shouldBe true
    } catch {
      case e: RuntimeException => log.error("Test failed during readychecks", e)
    } finally {
      Await.ready(containerManager.stopRmAll(), StopContainersTimeout)
    }
  }

  override def startAllOrFail(): Unit = {
    Await.result(containerManager.pullImages(), PullImagesTimeout)
    containerManager.initReadyAll(StartContainersTimeout).map(_.map(_._2).forall(identity))
    sys.addShutdownHook(
      containerManager.stopRmAll()
    )
  }
}
