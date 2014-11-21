package whisk.docker

import java.util.concurrent.atomic.AtomicBoolean

import com.github.dockerjava.api.DockerClient

import scala.concurrent.{ ExecutionContext, Future, Promise }

class DockerContainer(
    desc: DockerContainerDescription,
    links: Map[DockerContainer, DockerLink]) {

  private val idPromise = Promise[String]()
  private val isInitialized = new AtomicBoolean(false)

  {
    import scala.concurrent.ExecutionContext.Implicits.global
    idPromise.future.onComplete { t =>
      println("FUTURE IS COMPLETED WITH " + t)
    }
  }

  def init()(implicit dockerClient: DockerClient, ec: ExecutionContext): Future[String] =
    {
      require(!isInitialized.getAndSet(true), "Illegal state: container is already being initialized")
      for {
        s <- Future(desc(dockerClient.createContainerCmd(desc.image)).exec()).flatMap { resp =>
          if (resp.getWarnings != null && resp.getWarnings.length > 0) {
            idPromise.failure(new RuntimeException(s"Cannot run container ${desc.image}: ${resp.getWarnings.mkString(", ")}"))
          } else {
            idPromise.success(resp.getId)
          }
          id
        }
        _ <- Future(dockerClient.startContainerCmd(s).exec())
        // TODO: listen for events, catch when container is running
      } yield {
        println("returning id: " + s)
        s
      }
    }

  def stop()(implicit dockerClient: DockerClient, ec: ExecutionContext): Future[String] =
    for {
      s <- id
      _ <- Future(dockerClient.stopContainerCmd(s).exec())
    } yield s

  lazy val id: Future[String] = idPromise.future
}
