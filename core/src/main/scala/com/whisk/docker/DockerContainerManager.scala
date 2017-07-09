package com.whisk.docker

import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.concurrent.duration._

class DockerContainerManager(containers: Seq[DockerContainer], executor: DockerCommandExecutor)(
    implicit ec: ExecutionContext) {

  private lazy val log = LoggerFactory.getLogger(this.getClass)
  private implicit val dockerExecutor = executor

  private val dockerStatesMap: Map[DockerContainer, DockerContainerState] =
    containers.map(c => c -> new DockerContainerState(c))(collection.breakOut)

  val states = dockerStatesMap.values.toList

  def getContainerState(container: DockerContainer): DockerContainerState = {
    dockerStatesMap(container)
  }

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

  def initReadyAll(containerStartTimeout: Duration): Future[Seq[(DockerContainerState, Boolean)]] = {
    import DockerContainerManager._

    @tailrec
    def initGraph(graph: ContainerDependencyGraph,
                  previousInits: Future[Seq[DockerContainerState]] = Future.successful(Seq.empty))
      : Future[Seq[DockerContainerState]] = {
      val initializedContainers = previousInits.flatMap { prev =>
        Future.traverse(graph.containers.map(dockerStatesMap))(_.init()).map(prev ++ _)
      }

      graph.dependants match {
        case None => initializedContainers
        case Some(dependants) =>
          val readyInits: Future[Seq[Future[Boolean]]] =
            initializedContainers.map(_.map(state => state.isReady()))
          val simplifiedReadyInits: Future[Seq[Boolean]] = readyInits.flatMap(Future.sequence(_))
          Await.result(simplifiedReadyInits, containerStartTimeout)
          initGraph(dependants, initializedContainers)
      }
    }

    initGraph(buildDependencyGraph(containers)).flatMap(Future.traverse(_) { c =>
      c.isReady().map(c -> _).recover {
        case e =>
          log.error(e.getMessage, e)
          c -> false
      }
    })
  }

  def stopRmAll(): Future[Unit] = {
    val future = Future.traverse(states)(_.remove(force = true, removeVolumes = true)).map(_ => ())
    future.onComplete { _ =>
      executor.close()
    }
    future
  }

}

object DockerContainerManager {
  case class ContainerDependencyGraph(containers: Seq[DockerContainer],
                                      dependants: Option[ContainerDependencyGraph] = None)

  def buildDependencyGraph(containers: Seq[DockerContainer]): ContainerDependencyGraph = {
    @tailrec def buildDependencyGraph(graph: ContainerDependencyGraph): ContainerDependencyGraph =
      graph match {
        case ContainerDependencyGraph(containers, dependants) =>
          containers.partition(_.dependencies.isEmpty) match {
            case (containersWithoutLinks, Nil) => graph
            case (containersWithoutLinks, containersWithLinks) =>
              val linkedContainers = containers.foldLeft(Seq[DockerContainer]()) {
                case (links, container) => (links ++ container.dependencies)
              }
              val (containersWithLinksAndLinked, containersWithLinksNotLinked) =
                containersWithLinks.partition(linkedContainers.contains)
              val (containersToBeLeftAtCurrentPosition, containersToBeMovedUpALevel) = dependants
                .map(_.containers)
                .getOrElse(List.empty)
                .partition(
                    _.dependencies.exists(containersWithLinksNotLinked.contains)
                )

              buildDependencyGraph(
                  ContainerDependencyGraph(
                      containers = containersWithoutLinks ++ containersWithLinksAndLinked,
                      dependants = Some(
                          ContainerDependencyGraph(
                              containers = containersWithLinksNotLinked ++ containersToBeMovedUpALevel,
                              dependants = dependants.map(
                                  _.copy(containers = containersToBeLeftAtCurrentPosition))
                          ))
                  )
              )
          }
      }

    buildDependencyGraph(ContainerDependencyGraph(containers))
  }
}
