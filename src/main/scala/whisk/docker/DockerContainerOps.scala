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
      _ <- Future(dockerClient.startContainerCmd(s).exec())
    } yield s
  }

  def stop()(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    for {
      s <- id
      _ <- Future(dockerClient.stopContainerCmd(s).exec())
    } yield s

  def isRunning()(implicit dockerClient: DockerClient, ec: ExecutionContext) =
    for {
      s <- id
      resp <- Future(dockerClient.listContainersCmd().exec())
    } yield resp.exists(_.getId == s)

}
