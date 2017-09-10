//package com.whisk.docker
//
//trait DockerKafkaService extends DockerKit {
//
//  def KafkaAdvertisedPort = 9092
//  val ZookeeperDefaultPort = 2181
//
//  lazy val kafkaContainer = ContainerSpec("spotify/kafka")
//    .withPorts(KafkaAdvertisedPort -> Some(KafkaAdvertisedPort), ZookeeperDefaultPort -> None)
//    .withEnv(s"ADVERTISED_PORT=$KafkaAdvertisedPort", s"ADVERTISED_HOST=${dockerExecutor.host}")
//    .withReadyChecker(DockerReadyChecker.LogLineContains("kafka entered RUNNING state"))
//
//  abstract override def dockerContainers: List[ContainerSpec] =
//    kafkaContainer :: super.dockerContainers
//}
