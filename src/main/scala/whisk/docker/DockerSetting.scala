package whisk.docker

import com.github.dockerjava.api.DockerClient

import scala.concurrent.ExecutionContext

case class DockerSetting(
    containers: Map[DockerContainerDescription, DockerContainer] = Map.empty) {

  def +(desc: DockerContainerDescription): DockerSetting =
    copy(
      containers = containers + (desc -> new DockerContainer(desc, desc.links.map(kv => apply(kv._1) -> kv._2)))
    )

  def apply(desc: DockerContainerDescription): DockerContainer = containers.getOrElse(desc, {
    throw new IllegalArgumentException("Container not found for " + desc)
  })

  def init()(implicit dockerClient: DockerClient, ec: ExecutionContext): Unit = containers.foreach(_._2.init())

  def stop()(implicit dockerClient: DockerClient, ec: ExecutionContext): Unit = containers.foreach(_._2.stop())
}
