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
    val binds: Seq[String] = spec.volumeMappings.map { volumeMapping =>
      val rw = if (volumeMapping.rw) ":rw" else ""
      volumeMapping.host + ":" + volumeMapping.container + rw
    }

    val hostConfig = {
      val hostConfigBase =
        HostConfig.builder().portBindings(portBindings.asJava).binds(binds.asJava)

      val links = spec.links.map {
        case ContainerLink(container, alias) => s"${container.name.get}:$alias"
      }

      val hostConfigBuilder =
        if (links.isEmpty) hostConfigBase else hostConfigBase.links(links.asJava)
      hostConfigBuilder
        .withOption(spec.networkMode) {
          case (config, networkMode) => config.networkMode(networkMode)
        }
        .withOption(spec.hostConfig.flatMap(_.tmpfs)) {
          case (config, value) => config.tmpfs(value.asJava)
        }
        .withOption(spec.hostConfig.flatMap(_.memory)) {
          case (config, memory) => config.memory(memory)
        }
        .withOption(spec.hostConfig.flatMap(_.memoryReservation)) {
          case (config, reservation) => config.memoryReservation(reservation)
        }
        .privileged(spec.privileged.get)
        .build()
    }

    val containerConfig = ContainerConfig
      .builder()
      .image(spec.image)
      .hostConfig(hostConfig)
      .exposedPorts(spec.bindPorts.map(_._1.toString).toSeq: _*)
      .tty(spec.tty)
      .attachStdin(spec.stdinOpen)
      .env(spec.env: _*)
      .withOption(spec.user) { case (config, user) => config.user(user) }
      .withOption(spec.hostname) { case (config, hostname) => config.hostname(hostname) }
      .withOption(spec.command) { case (config, command) => config.cmd(command: _*) }
      .withOption(spec.entrypoint) {
        case (config, entrypoint) => config.entrypoint(entrypoint: _*)
      }
      .build()

    val creation = Future(
      spec.name.fold(client.createContainer(containerConfig))(
        client.createContainer(containerConfig, _))
    )

    creation.map(_.id)
  }

  override def startContainer(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.startContainer(id))
  }

  override def inspectContainer(id: String)(
      implicit ec: ExecutionContext): Future[Option[InspectContainerResult]] = {

    def inspect() =
      Future(client.inspectContainer(id))
        .flatMap { info =>
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

              val addresses: Iterable[String] = for {
                networks <- Option(info.networkSettings().networks()).map(_.asScala).toSeq
                (key, network) <- networks
                ip <- Option(network.ipAddress)
              } yield {
                ip
              }

              Future.successful(
                Some(
                  InspectContainerResult(info.state().running(),
                                         ports,
                                         info.name(),
                                         addresses.toSeq)))
            case None =>
              Future.failed(new Exception("can't extract ports"))
          }
        }
        .recover {
          case t: ContainerNotFoundException =>
            None
        }

    RetryUtils.looped(inspect(), attempts = 5, delay = FiniteDuration(1, TimeUnit.SECONDS))
  }

  override def withLogStreamLines(id: String, withErr: Boolean)(
      f: String => Unit)(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Unit = {
    val baseParams = List(AttachParameter.STDOUT, AttachParameter.STREAM, AttachParameter.LOGS)
    val logParams = if (withErr) AttachParameter.STDERR :: baseParams else baseParams
    val streamF = Future(client.attachContainer(id, logParams: _*))

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
    Future(
      client
        .listImages()
        .asScala
        .flatMap(img => Option(img.repoTags()).map(_.asScala).getOrElse(Seq.empty))
        .toSet)
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
