package com.whisk.docker

import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

class DockerContainerState(spec: DockerContainer) {

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

  private val _isReady = SinglePromise[Boolean]

  def isReady(): Future[Boolean] = _isReady.future

  def isRunning()(implicit docker: DockerCommandExecutor, ec: ExecutionContext, timeout: Duration): Future[Boolean] = {
    getRunningContainer().map(_.isDefined)
  }

  def init()(implicit docker: DockerCommandExecutor, ec: ExecutionContext, timeout: Duration): Future[this.type] = {
    for {
      s <- _id.init(docker.createContainer(spec))
      _ <- docker.startContainer(s)
    } yield {
      spec.logLineReceiver.foreach {
        case LogLineReceiver(withErr, f) => docker.withLogStreamLines(s, withErr)(f)
      }
      runReadyCheck
      this
    }
  }

  private def runReadyCheck()(implicit docker: DockerCommandExecutor,
                              ec: ExecutionContext, timeout: Duration): Future[Boolean] = {

    _isReady.init(
      (for {
        r <- isRunning() if r
        b <- spec.readyChecker(this) if b
      } yield b) recoverWith {
        case _: NoSuchElementException =>
          log.error("Not ready: " + {
            spec.image
          })
          Future.successful(false)
        case e =>
          log.error(e.getMessage, e)
          Future.successful(false)
      }
    )
  }

  protected def getRunningContainer()(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext, timeout: Duration): Future[Option[InspectContainerResult]] =
    id.flatMap(docker.inspectContainer)

  def getName()(implicit docker: DockerCommandExecutor, ec: ExecutionContext, timeout: Duration): Future[String] =
    getRunningContainer.flatMap {
      case Some(res) => Future.successful(res.name)
      case None      => Future.failed(new RuntimeException(s"Container ${spec.image} is not running"))
    }

  def getIpAddresses()(implicit docker: DockerCommandExecutor,
                       ec: ExecutionContext, timeout: Duration): Future[Seq[String]] = getRunningContainer.flatMap {
    case Some(res) => Future.successful(res.ipAddresses)
    case None      => Future.failed(new RuntimeException(s"Container ${spec.image} is not running"))
  }

  private val _ports = SinglePromise[Map[Int, Int]]

  def getPorts()(implicit docker: DockerCommandExecutor,
                 ec: ExecutionContext, timeout: Duration): Future[Map[Int, Int]] = {
    def portsFuture: Future[Map[Int, Int]] = getRunningContainer().flatMap {
      case None => Future.failed(new RuntimeException(s"Container ${spec.image} is not running"))
      case Some(c) =>
        val ports: Map[Int, Int] = c.ports.collect {
          case (exposedPort, Seq(binding, _*)) =>
            exposedPort.port -> binding.hostPort
        }
        Future.successful(ports)
    }
    _ports.init(portsFuture)
  }

  def remove(force: Boolean = true, removeVolumes: Boolean = true)(
      implicit docker: DockerCommandExecutor,
      ec: ExecutionContext, timeout: Duration): Future[Unit] =
    id.flatMap(x => docker.remove(x, force, removeVolumes))
}
