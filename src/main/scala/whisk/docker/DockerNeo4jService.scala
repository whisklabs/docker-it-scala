package whisk.docker

import org.scalatest.Suite

trait DockerNeo4jService extends DockerTestKit {
  self: Suite with DockerClientConfig =>

  val neo4jContainer = DockerContainer("tpires/neo4j")
    .withPorts(7474 -> None)
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(7474, "/db/data/"))

  abstract override def dockerContainers: List[DockerContainer] = neo4jContainer :: super.dockerContainers
}
