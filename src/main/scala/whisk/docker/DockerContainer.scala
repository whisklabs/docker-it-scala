package whisk.docker

import java.util.concurrent.atomic.AtomicBoolean

import com.github.dockerjava.api.command.CreateContainerCmd

import scala.concurrent.Promise

case class DockerContainer(
    image: String,
    command: Option[Seq[String]] = None,
    bindPorts: Map[Int, Option[Int]] = Map.empty,
    tty: Boolean = true,
    stdinOpen: Boolean = true) extends DockerContainerOps {

  private[docker] val idPromise = Promise[String]()
  private[docker] val isInitialized = new AtomicBoolean(false)

  def id = idPromise.future

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) = copy(bindPorts = ps.toMap)

  private[docker] def prepareCreateCmd(cmd: CreateContainerCmd): CreateContainerCmd =
    command
      .fold(cmd)(cmd.withCmd(_: _*))
      .withPortSpecs(bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
      .withTty(tty)
      .withStdinOpen(stdinOpen)

}