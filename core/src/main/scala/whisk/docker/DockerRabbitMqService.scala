package whisk.docker

trait DockerRabbitMqService extends DockerKit{

  val rabbitMqContainer = DockerContainer("rabbitmq:3.5")
                          .withReadyChecker(DockerReadyChecker.LogLine(_.contains("Starting broker")))
                          .withCommand("rabbitmq-server","--hostname","my-rabbit","--name","test-rabbit")

  abstract override def dockerContainers: List[DockerContainer] =
    rabbitMqContainer :: super.dockerContainers

}
