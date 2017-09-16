package com.whisk.docker.testkit.test

import java.util.{Collections, Properties}

import com.whisk.docker.testkit.{ContainerState, KafkaContainer}
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord, RecordMetadata}
import org.scalatest.{FunSuite, Suite}

class KafkaServiceTest extends FunSuite with DockerTestKitForAll {

  // binds to random port
  val kafkaContainer = new KafkaContainer(dockerClient.getHost, advertisedPort = Some(9092))

  override val managedContainers = kafkaContainer.toManagedContainer

  test("test container started") {
    assert(kafkaContainer.state().isInstanceOf[ContainerState.Ready], "kafka container is ready")
    assert(kafkaContainer.mappedPortOpt(kafkaContainer.KafkaPort).nonEmpty, "kafka port is exposed")

    val adminClient = kafkaContainer.createAdminClient()

    def topics() = adminClient.listTopics().names().get()

    assert(topics().isEmpty, "topics should be empty initially")

    val topicName = "my-topic"

    adminClient.createTopics(Collections.singletonList(new NewTopic(topicName, 1, 1))).all().get()

    assert(topics().contains(topicName))

    val producer = createProducer(kafkaContainer.bootstrapServers())

    val res: RecordMetadata =
      producer.send(new ProducerRecord[String, String](topicName, "some-value")).get()
    assert(res.topic() == topicName, "offset should be returned")

    adminClient.close()
    producer.close()
  }

  private def createProducer(bootstrapServers: String): Producer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("acks", "all")
    props.put("retries", Integer.valueOf(5))
    props.put("batch.size", Integer.valueOf(16384))
    props.put("linger.ms", Integer.valueOf(1))
    props.put("buffer.memory", Integer.valueOf(33554432))
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[String, String](props)
  }
}
