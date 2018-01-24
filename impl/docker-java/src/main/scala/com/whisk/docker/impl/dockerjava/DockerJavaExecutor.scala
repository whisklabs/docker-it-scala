package com.whisk.docker.impl.dockerjava

import java.util.concurrent.TimeUnit

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.{PortBinding => _, ContainerPort => _, _}
import com.github.dockerjava.core.command.{LogContainerResultCallback, PullImageResultCallback}
import com.google.common.io.Closeables
import com.whisk.docker._

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

class DockerJavaExecutor(override val host: String, client: DockerClient)
    extends DockerCommandExecutor {

  override def createContainer(spec: DockerContainer)(
      implicit ec: ExecutionContext): Future[String] = {
    val volumeToBind: Seq[(Volume, Bind)] = spec.volumeMappings.map { mapping =>
      val volume: Volume = new Volume(mapping.container)
      (volume, new Bind(mapping.host, volume, AccessMode.fromBoolean(mapping.rw)))
    }

    val hostConfig = new com.github.dockerjava.api.model.HostConfig()
      .withOption(spec.networkMode)({ case (config, value) => config.withNetworkMode(value) })
      .withPortBindings(spec.bindPorts.foldLeft(new Ports()) {
        case (ps, (guestPort, DockerPortMapping(Some(hostPort), address))) =>
          ps.bind(ExposedPort.tcp(guestPort), Ports.Binding.bindPort(hostPort))
          ps
        case (ps, (guestPort, DockerPortMapping(None, address))) =>
          ps.bind(ExposedPort.tcp(guestPort), Ports.Binding.empty())
          ps
      })
      .withLinks(
        new Links(spec.links.map {
          case ContainerLink(container, alias) =>
            new Link(container.name.get, alias)
        }: _*)
      )
      .withBinds(new Binds(volumeToBind.map(_._2): _*))
      .withOption(spec.hostConfig.flatMap(_.memory)) {
        case (config, memory) => config.withMemory(memory)
      }
      .withOption(spec.hostConfig.flatMap(_.memoryReservation)) {
        case (config, memoryReservation) => config.withMemoryReservation(memoryReservation)
      }

    val cmd = client
      .createContainerCmd(spec.image)
      .withHostConfig(hostConfig)
      .withPortSpecs(spec.bindPorts
        .map({
          case (guestPort, DockerPortMapping(Some(hostPort), address)) =>
            s"$address:$hostPort:$guestPort"
          case (guestPort, DockerPortMapping(None, address)) => s"$address::$guestPort"
        })
        .toSeq: _*)
      .withExposedPorts(spec.bindPorts.keys.map(ExposedPort.tcp).toSeq: _*)
      .withTty(spec.tty)
      .withStdinOpen(spec.stdinOpen)
      .withEnv(spec.env: _*)
      .withVolumes(volumeToBind.map(_._1): _*)
      .withOption(spec.user) { case (config, user) => config.withUser(user) }
      .withOption(spec.hostname) { case (config, hostName) => config.withHostName(hostName) }
      .withOption(spec.name) { case (config, name) => config.withName(name) }
      .withOption(spec.command) { case (config, c) => config.withCmd(c: _*) }
      .withOption(spec.entrypoint) {
        case (config, entrypoint) => config.withEntrypoint(entrypoint: _*)
      }
      .withPrivileged(spec.privileged.get)

    Future(cmd.exec()).map { resp =>
      if (resp.getId != null && resp.getId != "") {
        resp.getId
      } else {
        throw new RuntimeException(
          s"Cannot run container ${spec.image}: ${resp.getWarnings.mkString(", ")}")
      }
    }
  }

  override def startContainer(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.startContainerCmd(id).exec()).map(_ => ())
  }

  override def inspectContainer(id: String)(
      implicit ec: ExecutionContext): Future[Option[InspectContainerResult]] = {
    val resp = Future(Some(client.inspectContainerCmd(id).exec())).recover {
      case x: NotFoundException => None
    }
    val future = resp.map(_.map { result =>
      val containerBindings = Option(result.getNetworkSettings.getPorts)
        .map(_.getBindings.asScala.toMap)
        .getOrElse(Map())
      val portMap = containerBindings.collect {
        case (exposedPort, bindings) if Option(bindings).isDefined =>
          val p =
            ContainerPort(exposedPort.getPort,
                          PortProtocol.withName(exposedPort.getProtocol.toString.toUpperCase))
          val hostBindings: Seq[PortBinding] = bindings.map { b =>
            PortBinding(b.getHostIp, b.getHostPortSpec.toInt)
          }
          p -> hostBindings
      }

      val addresses: Iterable[String] = for {
        networks <- Option(result.getNetworkSettings.getNetworks).map(_.asScala).toSeq
        (key, network) <- networks
        ip <- Option(network.getIpAddress)
      } yield {
        ip
      }

      InspectContainerResult(running = true,
                             ports = portMap,
                             name = result.getName,
                             ipAddresses = addresses.toSeq)
    })
    RetryUtils.looped(
      future.flatMap {
        case Some(x) if x.running => Future.successful(Some(x))
        case None                 => Future.successful(None)
        case _                    => Future.failed(throw new Exception("container is not running"))
      },
      5,
      FiniteDuration(2, TimeUnit.SECONDS)
    )
  }

  override def withLogStreamLines(id: String, withErr: Boolean)(f: String => Unit)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext
  ): Unit = {
    val cmd =
      client.logContainerCmd(id).withStdOut(true).withStdErr(withErr).withFollowStream(true)

    cmd.exec(new LogContainerResultCallback {
      override def onNext(item: Frame): Unit = {
        super.onNext(item)
        f(s"[$id] ${item.toString}")
      }
    })
  }

  override def withLogStreamLinesRequirement(id: String, withErr: Boolean)(f: String => Boolean)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext): Future[Unit] = {

    val cmd =
      client.logContainerCmd(id).withStdOut(true).withStdErr(withErr).withFollowStream(true)
    for {

      res <- {
        val p = Promise[Unit]()
        cmd.exec(new LogContainerResultCallback {
          override def onNext(item: Frame): Unit = {
            super.onNext(item)
            if (f(item.toString)) {
              p.trySuccess(())
              onComplete()
            }
          }
        })
        p.future
      }
    } yield {
      res
    }
  }

  override def listImages()(implicit ec: ExecutionContext): Future[Set[String]] = {
    Future(
      client
        .listImagesCmd()
        .exec()
        .asScala
        .flatMap(img => Option(img.getRepoTags).getOrElse(Array()))
        .toSet)
  }

  override def pullImage(image: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.pullImageCmd(image).exec(new PullImageResultCallback()).awaitSuccess())
  }

  override def remove(id: String, force: Boolean, removeVolumes: Boolean)(
      implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.removeContainerCmd(id).withForce(force).withRemoveVolumes(true).exec())
  }

  override def close(): Unit = Closeables.close(client, true)
}
