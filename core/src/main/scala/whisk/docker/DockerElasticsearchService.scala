package whisk.docker

import scala.concurrent.duration._

trait DockerElasticsearchService extends DockerKit {

  val DefaultElasticsearchHttpPort = 9200
  val DefaultElasticsearchClientPort = 9300

  val elasticsearchContainer =
    configureDockerContainer("docker.elasticsearch")

  abstract override def dockerContainers: List[DockerContainer] =
    elasticsearchContainer :: super.dockerContainers
}
