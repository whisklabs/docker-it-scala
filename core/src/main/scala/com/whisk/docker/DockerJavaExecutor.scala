package com.whisk.docker

import java.util.concurrent.TimeUnit

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.command.{LogContainerResultCallback, PullImageResultCallback}
import com.google.common.io.Closeables

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

class DockerJavaExecutor(override val host: String, client: DockerClient)
    extends DockerCommandExecutor {

  override def createContainer(spec: DockerContainer)(
      implicit ec: ExecutionContext): Future[String] = {
    val volumeToBind: Seq[(Volume, Bind)] = spec.volumeMappings.map { mapping =>
      val volume: Volume = new Volume(mapping.container)
      (volume, new Bind(mapping.host, volume, AccessMode.fromBoolean(mapping.rw)))
    }

    val baseCmd = {
      val tmpCmd = client
      .createContainerCmd(spec.image)
      .withPortSpecs(spec.bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
      .withExposedPorts(spec.bindPorts.keys.map(ExposedPort.tcp).toSeq: _*)
      .withTty(spec.tty)
      .withStdinOpen(spec.stdinOpen)
      .withEnv(spec.env: _*)
      .withPortBindings(
          spec.bindPorts.foldLeft(new Ports()) {
            case (ps, (guestPort, Some(hostPort))) =>
              ps.bind(ExposedPort.tcp(guestPort), Ports.Binding.bindPort(hostPort))
              ps
            case (ps, (guestPort, None)) =>
              ps.bind(ExposedPort.tcp(guestPort), Ports.Binding.empty())
              ps
          }
      )
      .withLinks(
        spec.links.map{ case ContainerLink(container, alias) =>
          new Link(container.name.get, alias)
        }.asJava
      )
      .withVolumes(volumeToBind.map(_._1): _*)
      .withBinds(volumeToBind.map(_._2): _*)

      spec.name.map(tmpCmd.withName).getOrElse(tmpCmd)
    }

    val cmd = spec.command.fold(baseCmd)(c => baseCmd.withCmd(c: _*))
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
      InspectContainerResult(running = true, ports = portMap)
    })
    RetryUtils.looped(future.flatMap {
      case Some(x) if x.running => Future.successful(Some(x))
      case None => Future.successful(None)
      case _ => Future.failed(throw new Exception("container is not running"))
    }, 5, FiniteDuration(2, TimeUnit.SECONDS))
  }

  override def withLogStreamLines(id: String, withErr: Boolean)(f: String => Boolean)(
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
    Future(client.listImagesCmd().exec().asScala.flatMap(_.getRepoTags).toSet)
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
