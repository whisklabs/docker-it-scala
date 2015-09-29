package whisk.docker

import scala.concurrent.duration._

trait DockerNeo4jService extends DockerKit {

  val DefaultNeo4jHttpPort = 7474

  val neo4jContainer =
    configureDockerContainer("docker.neo4j")

  abstract override def dockerContainers: List[DockerContainer] =
    neo4jContainer :: super.dockerContainers
}
