package com.whisk.docker

import com.github.dockerjava.api.command.{ CreateContainerCmd, StartContainerCmd }
import com.github.dockerjava.api.model.{ Link, ExposedPort, Ports }

case class DockerContainer(
    image: String,
    command: Option[Seq[String]] = None,
    bindPorts: Map[Int, Option[Int]] = Map.empty,
    tty: Boolean = false,
    stdinOpen: Boolean = false,
    links: Map[DockerContainer, String] = Map.empty,
    env: Seq[String] = Seq.empty,
    readyChecker: DockerReadyChecker = DockerReadyChecker.Always) extends DockerContainerOps {

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) = copy(bindPorts = ps.toMap)

  def withLinks(links: (DockerContainer, String)*) = copy(links = links.toMap)

  def withReadyChecker(checker: DockerReadyChecker) = copy(readyChecker = checker)

  def withEnv(env: String*) = copy(env = env)

  private[docker] def prepareCreateCmd(cmd: CreateContainerCmd, links: Seq[Link]): CreateContainerCmd =
    command
      .fold(cmd)(cmd.withCmd(_: _*))
      .withPortSpecs(bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
      .withExposedPorts(bindPorts.keys.map(ExposedPort.tcp).toSeq: _*)
      .withTty(tty)
      .withStdinOpen(stdinOpen)
      .withEnv(env: _*)
      .withLinks(links: _*)
      .withPortBindings(
        bindPorts.foldLeft(new Ports()) {
          case (ps, (guestPort, Some(hostPort))) =>
            ps.bind(ExposedPort.tcp(guestPort), Ports.Binding(hostPort))
            ps
          case (ps, (guestPort, None)) =>
            ps.bind(ExposedPort.tcp(guestPort), new Ports.Binding())
            ps
        }
      )

}