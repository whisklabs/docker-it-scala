package whisk.docker

trait DockerMongoService extends DockerKit {

  val DefaultMongoClientPort = 27017

  val mongoContainer = DockerContainer("dockerfile/mongodb")
    .withPorts(DefaultMongoClientPort -> None)
    .withCommand("mongod", "--smallfiles")
    .withReadyChecker(
      DockerReadyChecker
        .LogLine(_.contains(" waiting for connections on port 27017"))
    )

  abstract override def dockerContainers = mongoContainer :: super.dockerContainers
}