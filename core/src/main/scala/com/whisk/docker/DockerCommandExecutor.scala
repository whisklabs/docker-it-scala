package com.whisk.docker

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

  def createContainer(spec: DockerContainer)(implicit ec: ExecutionContext): Future[String]

  def startContainer(id: String)(implicit ec: ExecutionContext): Future[Unit]

  def inspectContainer(id: String)(
      implicit ec: ExecutionContext): Future[Option[InspectContainerResult]]

  def withLogStreamLines(id: String, withErr: Boolean)(f: String => Unit)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext
  ): Unit

  def withLogStreamLinesRequirement(id: String, withErr: Boolean)(f: String => Boolean)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext): Future[Unit]

  def listImages()(implicit ec: ExecutionContext): Future[Set[String]]

  def pullImage(image: String)(implicit ec: ExecutionContext): Future[Unit]

  def remove(id: String, force: Boolean = true, removeVolumes: Boolean = true)(
      implicit ec: ExecutionContext): Future[Unit]

  def close(): Unit
}
