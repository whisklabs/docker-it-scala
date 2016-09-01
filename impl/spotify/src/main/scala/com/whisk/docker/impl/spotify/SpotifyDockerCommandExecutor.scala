package com.whisk.docker.impl.spotify

import java.nio.charset.StandardCharsets
import java.util
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

import com.google.common.io.Closeables
import com.spotify.docker.client.DockerClient.{AttachParameter, RemoveContainerParam}
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.messages.{ContainerConfig, HostConfig, PortBinding}
import com.spotify.docker.client.{DockerClient, LogMessage}
import com.whisk.docker._

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

class SpotifyDockerCommandExecutor(override val host: String, client: DockerClient)
    extends DockerCommandExecutor {

  override def createContainer(spec: DockerContainer)(
      implicit ec: ExecutionContext): Future[String] = {
    val portBindings: Map[String, util.List[PortBinding]] = spec.bindPorts.map {
      case (guestPort, DockerPortMapping(Some(hostPort), address)) =>
        guestPort.toString -> Collections.singletonList(PortBinding.of(address, hostPort))
      case (guestPort, DockerPortMapping(None, address)) =>
        guestPort.toString -> Collections.singletonList(PortBinding.randomPort(address))
    }

    val hostConfig = HostConfig.builder().portBindings(portBindings.asJava).build()

    val builder = ContainerConfig
      .builder()
      .image(spec.image)
      .hostConfig(hostConfig)
      .exposedPorts(spec.bindPorts.map(_._1.toString).toSeq: _*)
      .tty(spec.tty)
      .attachStdin(spec.stdinOpen)
      .env(spec.env: _*)

    val containerConfig = spec.command.fold(builder)(c => builder.cmd(c: _*)).build()

    val creation = Future(client.createContainer(containerConfig))

    creation.map(_.id)
  }

  override def startContainer(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.startContainer(id))
  }

  override def inspectContainer(id: String)(
      implicit ec: ExecutionContext): Future[Option[InspectContainerResult]] = {

    def inspect() =
      Future(client.inspectContainer(id)).flatMap { info =>
        val networkPorts = Option(info.networkSettings().ports())
        networkPorts match {
          case Some(p) =>
            val ports = info
              .networkSettings()
              .ports()
              .asScala
              .collect {
                case (cPort, bindings) if Option(bindings).exists(!_.isEmpty) =>
                  val binds = bindings.asScala
                    .map(b => com.whisk.docker.PortBinding(b.hostIp(), b.hostPort().toInt))
                    .toList
                  ContainerPort.parse(cPort) -> binds
              }
              .toMap
            Future.successful(Some(InspectContainerResult(info.state().running(), ports)))
          case None =>
            Future.failed(new Exception("can't extract ports"))
        }
      }.recover {
        case t: ContainerNotFoundException =>
          None
      }

    RetryUtils.looped(inspect(), attempts = 5, delay = FiniteDuration(1, TimeUnit.SECONDS))
  }

  override def withLogStreamLines(id: String, withErr: Boolean)(f: String => Unit)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext): Unit = {
    val baseParams = List(AttachParameter.STDOUT, AttachParameter.STREAM)
    val logParams = if (withErr) AttachParameter.STDERR :: baseParams else baseParams
    val streamF = Future(
        client.attachContainer(id,
                               logParams: _*))

    streamF.flatMap { stream =>
      Future {
        stream.forEachRemaining(new Consumer[LogMessage] {
          override def accept(t: LogMessage): Unit = {
            val str = StandardCharsets.US_ASCII.decode(t.content()).toString
            f(s"[$id] $str")
          }
        })
      }
    }
  }

  override def withLogStreamLinesRequirement(id: String, withErr: Boolean)(f: (String) => Boolean)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext): Future[Unit] = {
    
    val baseParams = List(AttachParameter.STDOUT, AttachParameter.STREAM, AttachParameter.LOGS)
    val logParams = if (withErr) AttachParameter.STDERR :: baseParams else baseParams
    
    val streamF = Future(client.attachContainer(id, logParams: _*))

    streamF.flatMap { stream =>
      val p = Promise[Unit]()
      Future {
        stream.forEachRemaining(new Consumer[LogMessage] {
          override def accept(t: LogMessage): Unit = {
            val str = StandardCharsets.US_ASCII.decode(t.content()).toString
            if (f(str)) {
              p.trySuccess(())
              Closeables.close(stream, true)
            }
          }
        })
      }
      p.future
    }
  }

  override def listImages()(implicit ec: ExecutionContext): Future[Set[String]] = {
    Future(client.listImages().asScala.flatMap(img => Option(img.repoTags()).map(_.asScala).getOrElse(Seq.empty)).toSet)
  }

  override def pullImage(image: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.pull(image))
  }

  override def remove(id: String, force: Boolean, removeVolumes: Boolean)(
      implicit ec: ExecutionContext): Future[Unit] = {
    Future(
        client.removeContainer(id,
                               RemoveContainerParam.forceKill(force),
                               RemoveContainerParam.removeVolumes(removeVolumes)))
  }

  override def close(): Unit = {
    Closeables.close(client, true)
  }
}
