package com.whisk.docker

import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.TimeUnit

import com.google.common.io.Closeables
import com.spotify.docker.client.DockerClient.{AttachParameter, RemoveContainerParam}
import com.spotify.docker.client.exceptions.ImageNotFoundException
import com.spotify.docker.client.messages._
import com.spotify.docker.client.{DockerClient, LogMessage, LogStream}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

class StartFailedException(msg: String) extends Exception(msg)

class ContainerCommandExecutor(val client: DockerClient) {

  def createContainer(spec: ContainerSpec)(
      implicit ec: ExecutionContext): Future[ContainerCreation] = {

    val portBindings: Map[String, java.util.List[PortBinding]] = spec.portBindings.map {
      case (guestPort, binding) =>
        guestPort.toString -> Collections.singletonList(binding)
    }
    val hostConfig =
      HostConfig
        .builder()
        .portBindings(portBindings.asJava)
        .build()

    val containerConfig = ContainerConfig
      .builder()
      .image(spec.image)
      .hostConfig(hostConfig)
      .exposedPorts(spec.portBindings.keySet.map(_.toString).asJava)
      .env(spec.env: _*)
      .withOption(spec.command) { case (config, command) => config.cmd(command: _*) }
      .build()

    Future(scala.concurrent.blocking(client.createContainer(containerConfig, spec.name.orNull)))
  }

  def startContainer(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(scala.concurrent.blocking(client.startContainer(id)))
  }

  def runningContainer(id: String)(implicit ec: ExecutionContext): Future[ContainerInfo] = {
    def inspect() = {
      Future(scala.concurrent.blocking(client.inspectContainer(id))).flatMap { info =>
        val status = info.state().status()
        val badStates = Set("removing", "paused", "exited", "dead")
        if (status == "running") {
          Future.successful(info)
        } else if (badStates(status)) {
          Future.failed(new StartFailedException("container is in unexpected state: " + status))
        } else {
          Future.failed(new Exception("not running yet"))
        }
      }
    }

    def attempt(rest: Int): Future[ContainerInfo] = {
      inspect().recoverWith {
        case e: StartFailedException => Future.failed(e)
        case _ if rest > 0 =>
          RetryUtils.withDelay(TimeUnit.SECONDS.toMillis(1))(attempt(rest - 1))
        case _ =>
          Future.failed(new StartFailedException("failed to get container in running state"))
      }
    }

    attempt(10)
  }

  private def logStreamFuture(id: String, withErr: Boolean)(
      implicit
      ec: ExecutionContext): Future[LogStream] = {
    val baseParams = List(AttachParameter.STDOUT, AttachParameter.STREAM, AttachParameter.LOGS)
    val logParams = if (withErr) AttachParameter.STDERR :: baseParams else baseParams
    Future(scala.concurrent.blocking(client.attachContainer(id, logParams: _*)))
  }

  def withLogStreamLines(id: String, withErr: Boolean)(f: String => Unit)(
      implicit ec: ExecutionContext): Unit = {

    logStreamFuture(id, withErr).foreach { stream =>
      stream.forEachRemaining(new java.util.function.Consumer[LogMessage] {

        override def accept(t: LogMessage): Unit = {
          val str = StandardCharsets.US_ASCII.decode(t.content()).toString
          f(s"[$id] $str")
        }
      })
    }
  }

  def withLogStreamLinesRequirement(id: String, withErr: Boolean)(f: (String) => Boolean)(
      implicit ec: ExecutionContext): Future[Unit] = {

    logStreamFuture(id, withErr).flatMap { stream =>
      val p = Promise[Unit]()
      Future {
        stream.forEachRemaining(new java.util.function.Consumer[LogMessage] {

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

  def remove(id: String, force: Boolean, removeVolumes: Boolean)(
      implicit ec: ExecutionContext): Future[Unit] = {
    Future(
      scala.concurrent.blocking(
        client.removeContainer(id,
                               RemoveContainerParam.forceKill(force),
                               RemoveContainerParam.removeVolumes(removeVolumes))))
  }

  def close(): Unit = {
    Closeables.close(client, true)
  }
}
