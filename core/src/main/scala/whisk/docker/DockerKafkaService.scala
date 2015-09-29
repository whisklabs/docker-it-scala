package whisk.docker

trait DockerKafkaService extends DockerKit with DockerZookeeperService {

  val kafkaContainer =
    configureDockerContainer("docker.kafka").withLinks(zookeeperContainer -> "zk")

  abstract override def dockerContainers: List[DockerContainer] =
    kafkaContainer :: super.dockerContainers
}
