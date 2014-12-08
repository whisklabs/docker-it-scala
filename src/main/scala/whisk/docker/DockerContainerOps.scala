package whisk.docker

import java.util.concurrent.atomic.AtomicBoolean

import com.spotify.docker.client.messages.ContainerInfo

import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future, Promise }

trait DockerContainerOps {
  self: DockerContainer =>

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

  def init()(implicit docker: Docker, ec: ExecutionContext): Future[this.type] =
    for {
      s <- _id.init(Future(docker.client.createContainer(prepareCreateCmd().build())).map { resp =>
        if (resp.id() != null && resp.id() != "") {
          resp.id()
        } else {
          throw new RuntimeException(s"Cannot run container $image: ${resp.getWarnings.mkString(", ")}")
        }
      })
      _ <- Future(docker.client.startContainer(s, prepareHostConfig().build()))
    } yield {
      this
    }

  def stop()(implicit docker: Docker, ec: ExecutionContext): Future[this.type] =
    for {
      s <- id
      _ <- Future(docker.client.stopContainer(s, 1))
    } yield this

  def remove(force: Boolean = false)(implicit docker: Docker, ec: ExecutionContext): Future[this.type] =
    for {
      s <- id
      _ <- Future(docker.client.stopContainer(s, 1))
      _ <- Future(docker.client.removeContainer(s))
    } yield this

  def isRunning()(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] =
    getRunningContainer().map(_.state().running())

  private val _isReady = SinglePromise[Boolean]

  def isReady()(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] =
    _isReady.init(
      (for {
        r <- isRunning() if r
        b <- readyChecker(this)
      } yield b) recover {
        case e =>
          System.err.println(e.getMessage)
          e.printStackTrace(System.err)
          false
      }
    )

  protected def getRunningContainer()(implicit docker: Docker, ec: ExecutionContext): Future[ContainerInfo] =
    for {
      s <- id
      resp <- Future(docker.client.inspectContainer(s))
    } yield resp

  private val _ports = SinglePromise[Map[Int, Int]]

  def getPorts()(implicit docker: Docker, ec: ExecutionContext): Future[Map[Int, Int]] =
    _ports.init(
      getRunningContainer().map { n =>
        Option(n.networkSettings().ports())
      }.collect {
        case Some(portMappings) =>
          portMappings.filter(_._2 != null).filter(_._2.nonEmpty).mapValues(_.head.hostPort().toInt).map {
            case (k, v) if k.endsWith("/tcp") || k.endsWith("/udp") => k.substring(0, -4).toInt -> v
            case (k, v) => k.toInt -> v
          }.toMap
        case None =>
          Map.empty
      }
    )
}
