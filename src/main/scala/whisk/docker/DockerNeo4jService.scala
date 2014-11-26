package whisk.docker

import org.scalatest.Suite
import scala.concurrent.duration._

trait DockerNeo4jService extends DockerTestKit {
  self: Suite with DockerConfig =>

  val neo4jContainer = DockerContainer("tpires/neo4j")
    .withPorts(7474 -> None)
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(7474, "/db/data/").within(100 millis).looped(12, 1250 millis))

  abstract override def dockerContainers: List[DockerContainer] = neo4jContainer :: super.dockerContainers
}
