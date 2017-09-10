package com.whisk.docker

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.google.common.collect.ImmutableList
import com.spotify.docker.client.exceptions.ImageNotFoundException
import com.spotify.docker.client.messages.ContainerCreation
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.JavaConverters._
import scala.language.postfixOps

trait ManagedContainers

case class SingleContainer(container: Container) extends ManagedContainers

case class ContainerGroup(containers: Seq[Container]) extends ManagedContainers {
  require(containers.nonEmpty, "container group should be non-empty")
}

object ContainerGroup {

  def of(containers: Container*): ContainerGroup = ContainerGroup(containers)
}

class DockerContainerManager(managedContainers: ManagedContainers,
                             executor: ContainerCommandExecutor,
                             dockerTestTimeouts: DockerTestTimeouts,
                             executionContext: ExecutionContext) {

  private implicit val ec: ExecutionContext = executionContext

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  private val registeredContainers = new ConcurrentHashMap[String, String]()

  private def waitUntilReady(container: Container): Future[Unit] = {
    container.spec.readyChecker match {
      case None =>
        Future.successful(())
      case Some(checker) =>
        checker(container)(executor, executionContext)
    }
  }

  private def printWarningsIfExist(creation: ContainerCreation): Unit = {
    Option(creation.warnings())
      .map(_.asScala.toList)
      .getOrElse(Nil)
      .foreach(w => log.warn(s"creating container: $w"))
  }

  private def ensureImage(image: String): Future[Unit] = {
    Future(scala.concurrent.blocking(executor.client.inspectImage(image)))
      .map(_ => ())
      .recoverWith {
        case x: ImageNotFoundException =>
          log.info(s"image [$image] not found. pulling...")
          Future(scala.concurrent.blocking(executor.client.pull(image)))
      }
  }

  //TODO log listeners
  def startContainer(container: Container): Future[Unit] = {
    val image = container.spec.image
    val startTime = System.nanoTime()
    log.debug("Starting container: {}", image)
    for {
      creation <- executor.createContainer(container.spec)
      id = creation.id()
      _ = registeredContainers.put(id, image)
      _ = container.created(id)
      _ = printWarningsIfExist(creation)
      _ = log.info(s"starting container with id: $id")
      _ <- executor.startContainer(id)
      _ = container.starting(id)
      _ = log.info(s"container is starting. id=$id")
      runningContainer <- executor.runningContainer(id)
      _ = log.debug(s"container entered running state. id=$id")
      _ = container.running(runningContainer)
      _ = log.debug(s"preparing to execute ready check for container")
      res <- waitUntilReady(container)
      _ = log.debug(s"container is ready. id=$id")
    } yield {
      container.ready(runningContainer)
      val timeTaken = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
      log.info(s"container $image is ready after ${timeTaken / 1000.0}s")
      res
    }

  }

  def start(): Unit = {
    log.debug("Starting containers")
    val containers: Seq[Container] = managedContainers match {
      case SingleContainer(c) => Seq(c)
      case ContainerGroup(cs) => cs
      case _                  => throw new Exception("unsupported type of managed containers")
    }

    val imagesF = Future.traverse(containers.map(_.spec.image))(ensureImage)
    Await.result(imagesF, dockerTestTimeouts.pull)

    val startedContainersF = Future.traverse(containers)(startContainer)

    sys.addShutdownHook(
      stop()
    )

    try {
      Await.result(startedContainersF, dockerTestTimeouts.init)
    } catch {
      case e: Exception =>
        log.error("Exception during container initialization", e)
        stop()
        throw new RuntimeException("Cannot run all required containers")
    }
  }

  def stop(): Unit = {
    try {
      Await.ready(stopRmAll(), dockerTestTimeouts.stop)
    } catch {
      case e: Throwable =>
        log.error(e.getMessage, e)
    }
  }

  def stopRmAll(): Future[Unit] = {
    val future = Future.traverse(registeredContainers.asScala) {
      case (cid, _) =>
        executor.remove(cid, force = true, removeVolumes = true)
    }
    future.onComplete { _ =>
      executor.close()
    }
    future.map(_ => ())
  }

}
