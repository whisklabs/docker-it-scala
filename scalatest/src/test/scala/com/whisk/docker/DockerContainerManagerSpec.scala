package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.impl.dockerjava._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import DockerContainerManager._

class DockerContainerManagerSpec extends WordSpecLike with Matchers {

  val container1 = DockerContainer("nginx:1.7.11", name = Some("container1"))
  val container2a = DockerContainer("nginx:1.7.11", name = Some("container2a")).withLinks(ContainerLink(container1, "container1"))
  val container2b = DockerContainer("nginx:1.7.11", name = Some("container2b")).withLinks(ContainerLink(container1, "container1"))
  val container3 = DockerContainer("nginx:1.7.11", name = Some("container3")).withLinks(ContainerLink(container2a, "container2a"))
  val container4 = DockerContainer("nginx:1.7.11", name = Some("container4")).withLinks(ContainerLink(container3, "container4"))
  val container5 = DockerContainer("nginx:1.7.11", name = Some("container5"))
  val containers = List(container1, container2a, container2b, container3, container4, container5)
   
  "The DockerContainerManager" should {
    "build a dependency graph from a list of containers" in {
      buildDependencyGraph(containers) shouldBe ContainerDependencyGraph(
        containers = Seq(container1, container5),
        dependants = Some(ContainerDependencyGraph(
          containers = Seq(container2a, container2b),
          dependants = Some(ContainerDependencyGraph(
            containers = Seq(container3),
            dependants = Some(ContainerDependencyGraph(
              containers = Seq(container4)
            ))
          ))
        ))
      )
    }

    "build the dependency graph from an empty list of containers" in {
      buildDependencyGraph(Seq.empty) shouldBe ContainerDependencyGraph(
        containers = Seq.empty,
        dependants = None
      )
    }

    "initialize all containers taking into account their dependencies" in {
      val dockerKit = new DockerKit with DockerKitDockerJava {
        override def dockerContainers = containers ++ super.dockerContainers
      }
      dockerKit.startAllOrFail()
      dockerKit.stopAllQuietly()
    }
  }
}
