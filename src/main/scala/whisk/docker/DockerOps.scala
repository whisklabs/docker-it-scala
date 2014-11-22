package whisk.docker

import com.github.dockerjava.api.DockerClient

import scala.concurrent.{ Future, ExecutionContext }

trait DockerOps {

  def init(containers: List[DockerContainer])(implicit dockerClient: DockerClient, ec: ExecutionContext): Future[Map[DockerContainer, String]] = {
    Future.sequence(
      containers.map { container =>
        if (container.isInitialized.getAndSet(true)) {
          throw new IllegalStateException("Container is already initialized: " + container)
        }
        for {
          id <- Future(container.prepareCreateCmd(dockerClient.createContainerCmd(container.image)).exec()).flatMap { resp =>
            if (resp.getId != null && resp.getId != "") {
              container.idPromise.success(resp.getId)
            } else {
              container.idPromise.failure(new RuntimeException(s"Cannot run container ${container.image}: ${resp.getWarnings.mkString(", ")}"))
            }
            container.id
          }
        } yield container -> id
      }).map(_.toMap)
  }

  def stop(containers: List[DockerContainer])(implicit dockerClient: DockerClient, ec: ExecutionContext): Future[List[String]] = {
    Future sequence containers.map { container =>
      for {
        id <- container.id
        _ <- Future(dockerClient.stopContainerCmd(id).exec())
      } yield id
    }
  }
}

object DockerOps extends DockerOps