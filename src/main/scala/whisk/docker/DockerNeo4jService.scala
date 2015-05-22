package whisk.docker

import scala.concurrent.duration._

trait DockerNeo4jService extends DockerKit {

  val DefaultNeo4jHttpPort = 7474

  val neo4jContainer = DockerContainer("whisk/neo4j:2.1.8")
    .withPorts(DefaultNeo4jHttpPort -> None)
    .withReadyChecker(
      DockerReadyChecker
        .HttpResponseCode(DefaultNeo4jHttpPort, "/db/data/")
        .within(100 millis)
        .looped(20, 1250 millis)
    )

  abstract override def dockerContainers: List[DockerContainer] = neo4jContainer :: super.dockerContainers
}
