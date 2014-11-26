package whisk.docker

import java.util.concurrent.atomic.AtomicBoolean

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

  def id = _id.future

  def init()(implicit docker: Docker, ec: ExecutionContext) =
    for {
      s <- _id.init(Future(prepareCreateCmd(docker.client.createContainerCmd(image)).exec()).map { resp =>
        if (resp.getId != null && resp.getId != "") {
          resp.getId
        } else {
          throw new RuntimeException(s"Cannot run container $image: ${resp.getWarnings.mkString(", ")}")
        }
      })
      _ <- Future(prepareStartCmd(docker.client.startContainerCmd(s)).exec())
    } yield this

  def stop()(implicit docker: Docker, ec: ExecutionContext) =
    for {
      s <- id
      _ <- Future(docker.client.stopContainerCmd(s).exec())
    } yield this

  def remove(force: Boolean = false)(implicit docker: Docker, ec: ExecutionContext) =
    for {
      s <- id
      _ <- Future(docker.client.removeContainerCmd(s).withForce(force).exec())
    } yield this

  def isRunning()(implicit docker: Docker, ec: ExecutionContext) =
    getRunningContainer().map(_.isDefined)

  private val _isReady = SinglePromise[Boolean]

  def isReady()(implicit docker: Docker, ec: ExecutionContext) =
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

  def getRunningContainer()(implicit docker: Docker, ec: ExecutionContext) =
    for {
      s <- id
      resp <- Future(docker.client.listContainersCmd().exec())
    } yield resp.find(_.getId == s)

  private val _ports = SinglePromise[Map[Int, Int]]

  def getPorts()(implicit docker: Docker, ec: ExecutionContext) =
    _ports.init(
      getRunningContainer()
        .map(_.map(_.getPorts.toSeq.filter(_.getPublicPort != null).map(p => p.getPrivatePort.toInt -> p.getPublicPort.toInt).toMap)
          .getOrElse(throw new RuntimeException(s"Container $image is not running")))
    )
}
