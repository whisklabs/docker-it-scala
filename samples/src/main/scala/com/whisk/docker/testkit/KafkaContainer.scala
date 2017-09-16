package com.whisk.docker.testkit

import java.util.Properties

import com.spotify.docker.client.messages.PortBinding
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}

class KafkaContainer(advertisedHost: String, advertisedPort: Option[Int]) extends BaseContainer {

  private lazy val _advertisedPort = advertisedPort.getOrElse(Helpers.newRandomPort())

  val KafkaPort = 9092
  val ZookeeperPort = 2181

  override val spec: ContainerSpec = ContainerSpec("spotify/kafka")
    .withPortBindings(KafkaPort -> PortBinding.of("0.0.0.0", _advertisedPort),
      ZookeeperPort -> PortBinding.randomPort("0.0.0.0"))
    .withEnv(s"ADVERTISED_PORT=${_advertisedPort}", s"ADVERTISED_HOST=$advertisedHost")
    .withReadyChecker(DockerReadyChecker.LogLineContains("kafka entered RUNNING state"))


  def bootstrapServers(): String = {
    s"$advertisedHost:${_advertisedPort}"
  }

  def createAdminClient(): AdminClient = {
    state() match {
      case s: ContainerState.IsRunning =>
        val props = new Properties()
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers())
        props.put(AdminClientConfig.CLIENT_ID_CONFIG, "test")
        props.put(AdminClientConfig.RECONNECT_BACKOFF_MS_CONFIG, Integer.valueOf(1000))
        props.put(AdminClientConfig.RETRIES_CONFIG, Integer.valueOf(5))
        AdminClient.create(props)
      case _ =>
        throw new Exception("can't create admin client for not running container")
    }
  }
}
