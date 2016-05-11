package com.whisk.docker

import java.util.concurrent.TimeUnit

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.{ExposedPort, Frame, Ports}
import com.github.dockerjava.core.command.{LogContainerResultCallback, PullImageResultCallback}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

class DockerJavaExecutor(override val host: String, client: DockerClient) extends DockerCommandExecutor {

  override def createContainer(spec: DockerContainer)(implicit ec: ExecutionContext): Future[String] = {
    val baseCmd =
      client.createContainerCmd(spec.image)
        .withPortSpecs(spec.bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
        .withExposedPorts(spec.bindPorts.keys.map(ExposedPort.tcp).toSeq: _*)
        .withTty(spec.tty)
        .withStdinOpen(spec.stdinOpen)
        .withEnv(spec.env: _*)
        .withPortBindings(
          spec.bindPorts.foldLeft(new Ports()) {
            case (ps, (guestPort, Some(hostPort))) =>
              ps.bind(ExposedPort.tcp(guestPort), Ports.binding(hostPort))
              ps
            case (ps, (guestPort, None)) =>
              ps.bind(ExposedPort.tcp(guestPort), new Ports.Binding())
              ps
          }
        )
    val cmd = spec.command.fold(baseCmd)(c => baseCmd.withCmd(c: _*))
    Future(cmd.exec()).map { resp =>
      if (resp.getId != null && resp.getId != "") {
        resp.getId
      } else {
        throw new RuntimeException(s"Cannot run container ${spec.image}: ${resp.getWarnings.mkString(", ")}")
      }
    }
  }

  override def startContainer(id: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.startContainerCmd(id).exec()).map(_ => ())
  }

  override def inspectContainer(id: String)(implicit ec: ExecutionContext): Future[Option[InspectContainerResult]] = {
    val resp = Future(Some(client.inspectContainerCmd(id).exec())).recover {
      case x: NotFoundException => None
    }
    val future = resp.map(_.map { result =>
      if(result.getState.getRunning) {
        val portMap = result.getNetworkSettings.getPorts.getBindings.asScala.toMap.collect { case (exposedPort, bindings) =>
          val p = ContainerPort(exposedPort.getPort, PortProtocol.withName(exposedPort.getProtocol.toString.toUpperCase))
          val hostBindings: Seq[PortBinding] = bindings.map { b =>
            PortBinding(b.getHostIp, b.getHostPort)
          }(collection.breakOut)
          p -> hostBindings
        }
        InspectContainerResult(running = true, ports = portMap)
      } else {
        InspectContainerResult(running = false, Map())
      }
    })
    RetryUtils.looped(future.flatMap {
      case Some(x) if x.running => Future.successful(Some(x))
      case None => Future.successful(None)
      case _ => Future.failed(throw new Exception("container is not running"))
    }, 5, FiniteDuration(2, TimeUnit.SECONDS))
  }

  override def withLogStreamLines(id: String, withErr: Boolean)(f: String => Boolean)(
    implicit docker: DockerCommandExecutor, ec: ExecutionContext): Future[Unit] = {

    val cmd = client.logContainerCmd(id).withStdOut(true).withStdErr(withErr).withFollowStream(true)
    for {

      res <- {
        val p = Promise[Unit]()
        cmd.exec(new LogContainerResultCallback {
          override def onNext(item: Frame): Unit = {
            super.onNext(item)
            if(f(item.toString)) {
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

  override def remove(id: String, force: Boolean, removeVolumes: Boolean)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(client.removeContainerCmd(id).withForce(force).withRemoveVolumes(true).exec())
  }
}
