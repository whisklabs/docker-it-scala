package com.whisk.docker.impl.spotify

import java.util
import java.util.Collections
import java.util.function.Consumer

import com.google.common.io.Closeables
import com.spotify.docker.client.DockerClient.{AttachParameter, RemoveContainerParam}
import com.spotify.docker.client.{DockerClient, LogMessage}
import com.spotify.docker.client.exceptions.ContainerNotFoundException
import com.spotify.docker.client.messages.{ContainerConfig, HostConfig, PortBinding}
import com.whisk.docker.{DockerCommandExecutor, DockerContainer, InspectContainerResult}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.JavaConverters._

class SpotifyDockerCommandExecutor(override val host: String, client: DockerClient) extends DockerCommandExecutor {

  override def createContainer(spec: DockerContainer)(implicit ec: ExecutionContext): Future[String] = {
    val portBindings: Map[String, util.List[PortBinding]] = spec.bindPorts.map {
      case (port, Some(bindTo)) =>
        port.toString -> Collections.singletonList(PortBinding.of("0.0.0.0", bindTo))
      case (port, None) =>
        port.toString -> Collections.singletonList(PortBinding.randomPort("0.0.0.0"))
    }

    val hostConfig = HostConfig.builder().portBindings(portBindings.asJava).build()

    val builder =
      ContainerConfig.builder()
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

  override def inspectContainer(id: String)(implicit ec: ExecutionContext): Future[Option[InspectContainerResult]] = {
    Future(client.inspectContainer(id)).flatMap { case info =>
      //       info.state()
      //      val containerBindings =
      //        info.networkSettings().portMapping().
      //          .getOrElse(Map())
      println(info)
      println(info.networkSettings().ports())
      Future.successful(Some(InspectContainerResult(info.state().running(), Map())))
    }.recover { case t: ContainerNotFoundException =>
      None
    }
  }

  override def withLogStreamLines(id: String, withErr: Boolean)(f: (String) => Boolean)(implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Unit] = {
    val streamF = Future(client.attachContainer(id, AttachParameter.STDOUT, AttachParameter.STREAM, AttachParameter.STDERR))

    streamF.flatMap { stream =>
      val p = Promise[Unit]()
      stream.forEachRemaining(new Consumer[LogMessage] {
        override def accept(t: LogMessage): Unit = {
          if (f(new String(t.content().array(), "utf-8"))) {
            p.trySuccess(())
            stream.close()
          }
        }
      })
      p.future
    }
  }

  override def listImages()(implicit ec: ExecutionContext): Future[Set[String]] = {
    Future(client.listImages().asScala.flatMap(_.repoTags().asScala).toSet)
  }

  override def pullImage(image: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.pull(image))
  }

  override def remove(id: String, force: Boolean, removeVolumes: Boolean)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.removeContainer(id, RemoveContainerParam.forceKill(force), RemoveContainerParam.removeVolumes(removeVolumes)))
  }

  override def close(): Unit = {
    Closeables.close(client, true)
  }
}
