package com.whisk.docker

import scala.concurrent.{ExecutionContext, Future}

object PortProtocol extends Enumeration {
  val TCP, UDP = Value
}

case class ContainerPort(port: Int, protocol: PortProtocol.Value)

case class PortBinding(hostIp: String, hostPort: Int)

case class InspectContainerResult(running: Boolean, ports: Map[ContainerPort, Seq[PortBinding]])

trait DockerCommandExecutor {

  def host: String

  def createContainer(spec: DockerContainer)(implicit ec: ExecutionContext): Future[String]

  def startContainer(id: String)(implicit ec: ExecutionContext): Future[Unit]

  def inspectContainer(id: String)(implicit ec: ExecutionContext): Future[Option[InspectContainerResult]]

  def withLogStreamLines(id: String, withErr: Boolean)(f: String => Boolean)(
    implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Unit]

  def listImages()(implicit ec: ExecutionContext): Future[Set[String]]

  def pullImage(image: String)(implicit ec: ExecutionContext): Future[Unit]

  def remove(id: String, force: Boolean = true, removeVolumes: Boolean = true)(implicit ec: ExecutionContext): Future[Unit]
}
