package whisk.docker.config

import whisk.docker.DockerContainer

trait DockerKafkaService extends DockerKitConfig with DockerZookeeperService {

  val kafkaContainer =
    configureDockerContainer("docker.kafka").withLinks(zookeeperContainer -> "zk")

  abstract override def dockerContainers: List[DockerContainer] =
    kafkaContainer :: super.dockerContainers
}
