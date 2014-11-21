package whisk.docker

import com.github.dockerjava.api.DockerClient

import scala.concurrent.{ Future, ExecutionContext, Promise }

class DockerContainer(
    desc: DockerContainerDescription,
    links: Map[DockerContainer, DockerLink]) {

  private val idPromise = Promise[String]()

  def init()(implicit dockerClient: DockerClient, ec: ExecutionContext): Future[String] = {
    for {
      resp <- Future(desc(dockerClient.createContainerCmd(desc.image)).exec())
      _ = resp.getWarnings.length match {
        case 0 =>
          idPromise.success(resp.getId)
        case n =>
          idPromise.failure(new RuntimeException(s"Cannot run container ${desc.image}: ${resp.getWarnings.mkString(", ")}"))
      }
      s <- id
      _ <- Future(dockerClient.startContainerCmd(s).exec())
      // TODO: listen for events, catch when container is running
    } yield s
  }

  def stop()(implicit dockerClient: DockerClient, ec: ExecutionContext): Future[String] = {
    for {
      s <- id
      _ <- Future(dockerClient.stopContainerCmd(s).exec())
    } yield s
  }

  lazy val id: Future[String] = idPromise.future
}
