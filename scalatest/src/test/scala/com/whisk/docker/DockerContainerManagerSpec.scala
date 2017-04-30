package com.whisk.docker

import com.whisk.docker.DockerContainerManager._
import com.whisk.docker.impl.dockerjava._
import org.scalatest._

class DockerContainerManagerSpec extends WordSpecLike with Matchers {


  "The DockerContainerManager" should {
    "a list of containers with dependencies" should {
      val linkedContainer1 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer1"))
      val linkedContainer2a = DockerContainer("nginx:1.7.11", name = Some("linkedContainer2a")).withLinks(ContainerLink(linkedContainer1, "linkedContainer1"))
      val linkedContainer2b = DockerContainer("nginx:1.7.11", name = Some("linkedContainer2b")).withLinks(ContainerLink(linkedContainer1, "linkedContainer1"))
      val linkedContainer3 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer3")).withLinks(ContainerLink(linkedContainer2a, "linkedContainer2a"))
      val linkedContainer4 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer4")).withLinks(ContainerLink(linkedContainer3, "linkedContainer4"))
      val linkedContainer5 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer5"))
      val linkedContainers = List(linkedContainer1, linkedContainer2a, linkedContainer2b, linkedContainer3, linkedContainer4, linkedContainer5)

      "build a dependency graph from a list of containers with dependencies" in {
        buildDependencyGraph(linkedContainers) shouldBe ContainerDependencyGraph(
          containers = Seq(linkedContainer1, linkedContainer5),
          dependants = Some(ContainerDependencyGraph(
            containers = Seq(linkedContainer2a, linkedContainer2b),
            dependants = Some(ContainerDependencyGraph(
              containers = Seq(linkedContainer3),
              dependants = Some(ContainerDependencyGraph(
                containers = Seq(linkedContainer4)
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
          override def dockerContainers = linkedContainers ++ super.dockerContainers
        }
        dockerKit.startAllOrFail()
        dockerKit.stopAllQuietly()
      }
    }

    "a list of containers with links" should {

      val unlinkedContainer1 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer1"))
      val unlinkedContainer2a = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer2a")).withUnlinkedDependencies(unlinkedContainer1)
      val unlinkedContainer2b = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer2b")).withUnlinkedDependencies(unlinkedContainer1)
      val unlinkedContainer3 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer3")).withUnlinkedDependencies(unlinkedContainer2a)
      val unlinkedContainer4 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer4")).withUnlinkedDependencies(unlinkedContainer3)
      val unlinkedContainer5 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer5"))
      val unlinkedContainers = List(unlinkedContainer1, unlinkedContainer2a, unlinkedContainer2b, unlinkedContainer3, unlinkedContainer4, unlinkedContainer5)

      "build a dependency graph from a list of containers" in {
        buildDependencyGraph(unlinkedContainers) shouldBe ContainerDependencyGraph(
          containers = Seq(unlinkedContainer1, unlinkedContainer5),
          dependants = Some(ContainerDependencyGraph(
            containers = Seq(unlinkedContainer2a, unlinkedContainer2b),
            dependants = Some(ContainerDependencyGraph(
              containers = Seq(unlinkedContainer3),
              dependants = Some(ContainerDependencyGraph(
                containers = Seq(unlinkedContainer4)
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
          override def dockerContainers = unlinkedContainers ++ super.dockerContainers
        }
        dockerKit.startAllOrFail()
        dockerKit.stopAllQuietly()
      }
    }
  }
}
