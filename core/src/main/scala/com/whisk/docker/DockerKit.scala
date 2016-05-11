package com.whisk.docker

import java.util.concurrent.Executors

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

  def dockerContainers: List[DockerContainer] = Nil

  // we need ExecutionContext in order to run docker.init() / docker.stop() there
  implicit lazy val dockerExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(dockerContainers.length * 2))
  implicit lazy val dockerExecutor = new DockerJavaExecutor(docker.host, docker.client)

  lazy val containerManager = new DockerContainerManager(dockerContainers, dockerExecutor)

  def isContainerReady(container: DockerContainer): Future[Boolean] =
    containerManager.isReady(container)

  def startAllOrFail(): Unit = {
    Await.result(containerManager.pullImages(), PullImagesTimeout)
    val allRunning: Boolean = try {
      val future: Future[Boolean] = containerManager.initReadyAll().map(_.map(_._2).forall(identity))
      sys.addShutdownHook(
        containerManager.stopRmAll()
      )
      Await.result(future, StartContainersTimeout)
    } catch {
      case e: Exception =>
        log.error("Exception during container initialization", e)
        false
    }
    if (!allRunning) {
      Await.ready(containerManager.stopRmAll(), StopContainersTimeout)
      throw new RuntimeException("Cannot run all required containers")
    }
  }

  def stopAllQuietly(): Unit = {
    try {
      Await.ready(containerManager.stopRmAll(), StopContainersTimeout)
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
    }
  }

}
