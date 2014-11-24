package whisk.docker

import com.github.dockerjava.api.DockerClient

import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }

trait DockerContainerOps {
  self: DockerContainer =>

  def init()(implicit dockerClient: DockerClient, ec: ExecutionContext) = {
    if (isInitialized.getAndSet(true)) {
      throw new IllegalStateException("Container is already initialized: " + this)
    }
    for {
      s <- Future(prepareCreateCmd(dockerClient.createContainerCmd(image)).exec()).flatMap { resp =>
        if (resp.getId != null && resp.getId != "") {
          idPromise.success(resp.getId)
        } else {
          idPromise.failure(new RuntimeException(s"Cannot run container $image: ${resp.getWarnings.mkString(", ")}"))
        }
        id
      }
      _ <- Future(prepareStartCmd(dockerClient.startContainerCmd(s)).exec())
    } yield this
  }

  def stop()(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    for {
      s <- id
      _ <- Future(dockerClient.stopContainerCmd(s).exec())
    } yield this

  def remove(force: Boolean = false)(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    for {
      s <- id
      _ <- Future(dockerClient.removeContainerCmd(s).withForce(force).exec())
    } yield this

  def isRunning()(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    getRunningContainer().map(_.isDefined)

  def isReady()(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    (for {
      r <- isRunning() if r
      b <- readyChecker(this)
    } yield b) recover {
      case e =>
        false
    }

  def getRunningContainer()(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    for {
      s <- id
      resp <- Future(dockerClient.listContainersCmd().exec())
    } yield resp.find(_.getId == s)

  def getPorts()(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    getRunningContainer().map(_.map(_.getPorts.toSeq.map(p => p.getPrivatePort -> p.getPublicPort).toMap).getOrElse(throw new RuntimeException(s"Container $image is not running")))
}
