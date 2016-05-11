package com.whisk.docker

import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class DockerContainerManager(containers: Seq[DockerContainer], executor: DockerCommandExecutor)(implicit ec: ExecutionContext) {

  private lazy val log = LoggerFactory.getLogger(this.getClass)
  private implicit val dockerExecutor = executor

  private val dockerStatesMap: Map[DockerContainer, DockerContainerState] =
    containers.map(c => c -> new DockerContainerState(c))(collection.breakOut)

  val states = dockerStatesMap.values.toList

  def isReady(container: DockerContainer): Future[Boolean] = {
    dockerStatesMap(container).isReady()
  }

  def pullImages(): Future[Seq[String]] = {
    executor.listImages().flatMap { images =>
      val imagesToPull: Seq[String] = containers.map(_.image).filterNot { image =>
        val cImage = if (image.contains(":")) image else image + ":latest"
        images(cImage)
      }
      Future.traverse(imagesToPull)(i => executor.pullImage(i)).map(_ => imagesToPull)
    }
  }

  def initReadyAll(): Future[Seq[(DockerContainerState, Boolean)]] =
    Future.traverse(states)(_.init()).flatMap(Future.traverse(_)(c => c.isReady().map(c -> _).recover {
      case e =>
        log.error(e.getMessage, e)
        c -> false
    }))

  def stopRmAll(): Future[Unit] =
    Future.traverse(states)(_.remove(force = true, removeVolumes = true)).map(_ => ())

}
