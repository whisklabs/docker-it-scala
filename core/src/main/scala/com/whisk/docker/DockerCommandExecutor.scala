package com.whisk.docker

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

object PortProtocol extends Enumeration {
  val TCP, UDP = Value
}

case class ContainerPort(port: Int, protocol: PortProtocol.Value)

object ContainerPort {
  def parse(str: String) = {
    val Array(p, rest @ _*) = str.split("/")
    val proto = rest.headOption
      .flatMap(pr => PortProtocol.values.find(_.toString.equalsIgnoreCase(pr)))
      .getOrElse(PortProtocol.TCP)
    ContainerPort(p.toInt, proto)
  }
}

case class PortBinding(hostIp: String, hostPort: Int)

case class InspectContainerResult(running: Boolean,
                                  ports: Map[ContainerPort, Seq[PortBinding]],
                                  name: String,
                                  ipAddresses: Seq[String])

trait DockerCommandExecutor {

  def host: String

  def createContainer(spec: DockerContainer)(implicit ec: ExecutionContext, timeout: Duration): Future[String]

  def startContainer(id: String)(implicit ec: ExecutionContext, timeout: Duration): Future[Unit]

  def inspectContainer(id: String)(
      implicit ec: ExecutionContext, timeout: Duration): Future[Option[InspectContainerResult]]

  def withLogStreamLines(id: String, withErr: Boolean)(f: String => Unit)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext, timeout: Duration
  ): Unit

  def withLogStreamLinesRequirement(id: String, withErr: Boolean)(f: String => Boolean)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext, timeout: Duration): Future[Unit]

  def listImages()(implicit ec: ExecutionContext, timeout: Duration): Future[Set[String]]

  def pullImage(image: String)(implicit ec: ExecutionContext, timeout: Duration): Future[Unit]

  def remove(id: String, force: Boolean = true, removeVolumes: Boolean = true)(
      implicit ec: ExecutionContext, timeout: Duration): Future[Unit]

  def close(): Unit
}
