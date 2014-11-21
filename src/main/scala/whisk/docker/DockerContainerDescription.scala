package whisk.docker

import com.github.dockerjava.api.command.CreateContainerCmd

case class DockerContainerDescription(
    image: String,
    command: Option[Seq[String]] = None,
    links: Map[DockerContainerDescription, DockerLink] = Map.empty,
    bindPorts: Map[Int, Option[Int]] = Map.empty) {

  def apply(cmd: CreateContainerCmd): CreateContainerCmd = {
    command.fold(cmd)(cmd.withCmd(_: _*)).withPortSpecs(bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
  }

  def withCommand(cmd: String*) = copy(command = Some(cmd))

}
