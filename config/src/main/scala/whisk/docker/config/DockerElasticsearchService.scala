package whisk.docker.config

import whisk.docker.DockerContainer

trait DockerElasticsearchService extends DockerKitConfig {

  val elasticsearchContainer =
    configureDockerContainer("docker.elasticsearch")

  abstract override def dockerContainers: List[DockerContainer] =
    elasticsearchContainer :: super.dockerContainers
}
