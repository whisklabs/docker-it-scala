package whisk.docker

import com.google.common.collect.Lists
import com.spotify.docker.client.messages.{ PortBinding, HostConfig, ContainerConfig }

case class DockerContainer(
    image: String,
    command: Option[Seq[String]] = None,
    bindPorts: Map[Int, Option[Int]] = Map.empty,

    tty: Boolean = false,
    stdinOpen: Boolean = false,

    readyChecker: DockerReadyChecker = DockerReadyChecker.Always) extends DockerContainerOps {

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) = copy(bindPorts = ps.toMap)

  def withReadyChecker(checker: DockerReadyChecker) = copy(readyChecker = checker)

  private[docker] def prepareCreateCmd(builder: ContainerConfig.Builder = ContainerConfig.builder()): ContainerConfig.Builder =
    command
      .fold(builder)(builder.cmd(_: _*))
      .portSpecs(bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
      .exposedPorts(bindPorts.keys.toSeq.map(_.toString): _*)
      .tty(tty)
      .attachStdin(stdinOpen)
      .image(image)

  private[docker] def prepareHostConfig(builder: HostConfig.Builder = HostConfig.builder()): HostConfig.Builder =
    {
      import collection.JavaConversions._
      builder.portBindings(mapAsJavaMap(bindPorts.map(kv => kv._1.toString -> Lists.newArrayList(PortBinding.of("", kv._2.fold("")(_.toString))))))
    }
}