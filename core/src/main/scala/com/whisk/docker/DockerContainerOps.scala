package com.whisk.docker

import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

import com.github.dockerjava.api.NotModifiedException
import com.github.dockerjava.api.model.{Frame, Container, Link}
import com.github.dockerjava.core.command.{LogContainerResultCallback, PullImageResultCallback}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, Promise}

trait DockerContainerOps {
  self: DockerContainer =>

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  class SinglePromise[T] {
    val promise = Promise[T]()

    def future = promise.future

    val flag = new AtomicBoolean(false)

    def init(f: => Future[T]): Future[T] = {
      if (!flag.getAndSet(true)) {
        promise.tryCompleteWith(f)
      }
      future
    }
  }

  object SinglePromise {
    def apply[T] = new SinglePromise[T]
  }

  private val _id = SinglePromise[String]

  def id: Future[String] = _id.future

  private val _image = SinglePromise[Unit]

  def pull()(implicit docker: Docker, ec: ExecutionContext): Future[this.type] =
    for {
      _ <- _image.init(Future(docker.client.pullImageCmd(image).exec(new PullImageResultCallback()).awaitSuccess()))
    } yield this

  def init()(implicit docker: Docker, ec: ExecutionContext): Future[this.type] = {
    val linksF = Future.traverse(links) {
      case (container, alias) =>
        for {
          _ <- container.isReady()
          c <- container.getRunningContainer()
        } yield {
          val linkedContainerName =
            c.flatMap(_.getNames.headOption).getOrElse(
              throw new RuntimeException(s"Cannot find linked container $alias"))
          new Link(linkedContainerName, alias)
        }
    }
    for {
      links <- linksF
      s <- _id.init(Future(prepareCreateCmd(docker.client.createContainerCmd(image), links.toSeq).exec()).map { resp =>
        if (resp.getId != null && resp.getId != "") {
          resp.getId
        } else {
          throw new RuntimeException(s"Cannot run container $image: ${resp.getWarnings.mkString(", ")}")
        }
      })
      _ <- Future(docker.client.startContainerCmd(s).exec())
    } yield {
      runReadyCheck
      this
    }
  }

  def stop()(implicit docker: Docker, ec: ExecutionContext): Future[this.type] =
    for {
      s <- id
      _ <- Future(docker.client.stopContainerCmd(s).exec()) recover {
        case _: NotModifiedException =>
          true
      }
    } yield this

  def remove(force: Boolean = true, removeVolumes: Boolean = true)(implicit docker: Docker, ec: ExecutionContext): Future[this.type] =
    for {
      s <- id
      _ <- Future(docker.client.removeContainerCmd(s).withForce(force).withRemoveVolumes(true).exec())
    } yield this

  def isRunning()(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] =
    getRunningContainer().map(_.isDefined)

  private val _isReady = SinglePromise[Boolean]

  def isReady(): Future[Boolean] = _isReady.future

  private def runReadyCheck()(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] =
    _isReady.init(
      (for {
        r <- isRunning() if r
        b <- readyChecker(this) if b
      } yield b) recoverWith {
        case _: NoSuchElementException =>
          log.error("Not ready: " + image)
          Future.successful(false)
        case e =>
          log.error(e.getMessage, e)
          Future.successful(false)
      }
    )

  def withLogStreamLines[T](withErr: Boolean)(f: PartialFunction[Frame, T])(implicit docker: Docker, ec: ExecutionContext): Future[T] = {
    for {
      s <- id
      baseCmd = docker.client.logContainerCmd(s).withStdOut().withFollowStream()
      cmd = if (withErr) baseCmd.withStdErr() else baseCmd
      res <- {
        val p = Promise[T]()
        cmd.exec(new LogContainerResultCallback {
          override def onNext(item: Frame): Unit = {
            super.onNext(item)
            if(f.isDefinedAt(item)) {
              p.trySuccess(f.apply(item))
            }
          }
        })
        p.future
      }
    } yield {
      res
    }
  }

  private def linesFromIS(is: InputStream): Iterator[String] = {
    scala.io.Source.fromInputStream(is)(scala.io.Codec.ISO8859).getLines()
  }

  protected def getRunningContainer()(implicit docker: Docker, ec: ExecutionContext): Future[Option[Container]] =
    for {
      s <- id
      resp <- Future(docker.client.listContainersCmd().exec())
    } yield resp.find(_.getId == s)

  private val _ports = SinglePromise[Map[Int, Int]]

  def getPorts()(implicit docker: Docker, ec: ExecutionContext): Future[Map[Int, Int]] =
    _ports.init(
      getRunningContainer()
        .map(_.map(_.getPorts.toSeq.filter(_.getPublicPort != null).map(p => p.getPrivatePort.toInt -> p.getPublicPort.toInt).toMap)
          .getOrElse(throw new RuntimeException(s"Container $image is not running")))
    )
}
