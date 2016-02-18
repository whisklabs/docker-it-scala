package com.whisk.docker

import com.github.dockerjava.core.DockerClientConfig
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait DockerKit {
  implicit val docker: Docker = new Docker(DockerClientConfig.createDefaultConfigBuilder().build())

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  val PullImagesTimeout = 20.minutes
  val StartContainersTimeout = 20.seconds
  val StopContainersTimeout = 10.seconds

  // we need ExecutionContext in order to run docker.init() / docker.stop() there
  implicit def dockerExecutionContext: ExecutionContext = ExecutionContext.global

  def dockerContainers: List[DockerContainer] = Nil

  def listImages(): Future[Set[String]] = {
    import scala.collection.JavaConverters._
    Future(docker.client.listImagesCmd().exec().asScala.flatMap(_.getRepoTags).toSet)
  }

  def stopRmAll(): Future[Seq[DockerContainer]] =
    Future.traverse(dockerContainers)(_.remove(force = true))

  def pullImages(): Future[Seq[DockerContainer]] = {
    listImages().flatMap { images =>
      val containersToPull = dockerContainers.filterNot { c =>
        val cImage = if (c.image.contains(":")) c.image else c.image + ":latest"
        images(cImage)
      }
      Future.traverse(containersToPull)(_.pull())
    }
  }

  def initReadyAll(): Future[Seq[(DockerContainer, Boolean)]] =
    Future.traverse(dockerContainers)(_.init()).flatMap(Future.traverse(_)(c => c.isReady.map(c -> _).recover {
      case e =>
        log.error(e.getMessage, e)
        c -> false
    }))


  def startAllOrFail(): Unit = {
    Await.result(pullImages(), PullImagesTimeout)
    val allRunning: Boolean = try {
      val future: Future[Boolean] = initReadyAll().map(_.map(_._2).forall(identity))
      Await.result(future, StartContainersTimeout)
    } catch {
      case e: Exception =>
        log.error("Exception during container initialization", e)
        false
    }
    if (!allRunning) {
      Await.ready(stopRmAll(), StopContainersTimeout)
      throw new RuntimeException("Cannot run all required containers")
    }
  }

  def stopAllQuietly(): Unit = {
    try {
      Await.ready(stopRmAll(), StopContainersTimeout)
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
    }
  }

}
