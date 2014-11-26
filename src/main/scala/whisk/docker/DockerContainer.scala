package whisk.docker

import java.util.concurrent.atomic.AtomicBoolean

import com.github.dockerjava.api.command.{ CreateContainerCmd, StartContainerCmd }
import com.github.dockerjava.api.model.{ ExposedPort, Ports }

import scala.concurrent.Promise

case class DockerContainer(
    image: String,
    command: Option[Seq[String]] = None,
    bindPorts: Map[Int, Option[Int]] = Map.empty,
    tty: Boolean = false,
    stdinOpen: Boolean = false,

    readyChecker: DockerReadyChecker = DockerReadyChecker.Always) extends DockerContainerOps {

  private[docker] val idPromise = Promise[String]()
  private[docker] val isInitialized = new AtomicBoolean(false)

  def id = idPromise.future

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) = copy(bindPorts = ps.toMap)

  def withReadyChecker(checker: DockerReadyChecker) = copy(readyChecker = checker)

  private[docker] def prepareCreateCmd(cmd: CreateContainerCmd): CreateContainerCmd =
    command
      .fold(cmd)(cmd.withCmd(_: _*))
      .withPortSpecs(bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
      .withExposedPorts(bindPorts.keys.map(ExposedPort.tcp).toSeq: _*)
      .withTty(tty)
      .withStdinOpen(stdinOpen)

  private[docker] def prepareStartCmd(cmd: StartContainerCmd): StartContainerCmd =
    cmd
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