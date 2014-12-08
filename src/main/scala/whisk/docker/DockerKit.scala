package whisk.docker

import scala.concurrent.{ Future, ExecutionContext }

trait DockerKit {
  implicit lazy val docker: Docker = new Docker()

  // we need ExecutionContext in order to run docker.init() / docker.stop() there
  implicit def dockerExecutionContext: ExecutionContext = ExecutionContext.global

  def dockerContainers: List[DockerContainer] = Nil

  def stopRmAll(): Future[Seq[DockerContainer]] =
    Future.traverse(dockerContainers)(_.remove(force = true))

  def initReadyAll(): Future[Seq[(DockerContainer, Boolean)]] =
    Future.traverse(dockerContainers)(_.init()).flatMap(Future.traverse(_)(c => c.isReady().map(c -> _).recover {
      case e =>
        System.err.println(e.getMessage)
        e.printStackTrace(System.err)
        c -> false
    }))
}
