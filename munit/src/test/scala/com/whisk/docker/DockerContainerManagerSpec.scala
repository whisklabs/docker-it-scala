package com.whisk.docker

import com.whisk.docker.DockerContainerManager._
import com.whisk.docker.impl.dockerjava._
import _root_.munit.FunSuite

class DockerContainerManagerSpec extends FunSuite {
  val linkedContainer1 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer1"))
  val linkedContainer2a = DockerContainer("nginx:1.7.11", name = Some("linkedContainer2a"))
    .withLinks(ContainerLink(linkedContainer1, "linkedContainer1"))
  val linkedContainer2b = DockerContainer("nginx:1.7.11", name = Some("linkedContainer2b"))
    .withLinks(ContainerLink(linkedContainer1, "linkedContainer1"))
  val linkedContainer3 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer3"))
    .withLinks(ContainerLink(linkedContainer2a, "linkedContainer2a"))
  val linkedContainer4 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer4"))
    .withLinks(ContainerLink(linkedContainer3, "linkedContainer4"))
  val linkedContainer5 = DockerContainer("nginx:1.7.11", name = Some("linkedContainer5"))
  val linkedContainers = List(linkedContainer1,
                              linkedContainer2a,
                              linkedContainer2b,
                              linkedContainer3,
                              linkedContainer4,
                              linkedContainer5)

  test("a list of containers with deps - build a dep graph from a list of containers with deps") {
    assertEquals(buildDependencyGraph(linkedContainers), ContainerDependencyGraph(
      containers = Seq(linkedContainer1, linkedContainer5),
      dependants = Some(
        ContainerDependencyGraph(
          containers = Seq(linkedContainer2a, linkedContainer2b),
          dependants = Some(
            ContainerDependencyGraph(
              containers = Seq(linkedContainer3),
              dependants = Some(ContainerDependencyGraph(
                containers = Seq(linkedContainer4)
              ))
            ))
        ))
    ))
  }

  test("a list of containers with deps - build the dep graph from an empty list of containers") {
    assertEquals(buildDependencyGraph(Seq.empty), ContainerDependencyGraph(
      containers = Seq.empty,
      dependants = None
    ))
  }

  test("a list of containers with deps - init all containers taking into account their deps") {
    val dockerKit = new DockerKit with DockerKitDockerJava {
      override def dockerContainers = linkedContainers ++ super.dockerContainers
    }
    dockerKit.startAllOrFail()
    dockerKit.stopAllQuietly()
  }

  val unlinkedContainer1 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer1"))
  val unlinkedContainer2a = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer2a"))
    .withUnlinkedDependencies(unlinkedContainer1)
  val unlinkedContainer2b = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer2b"))
    .withUnlinkedDependencies(unlinkedContainer1)
  val unlinkedContainer3 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer3"))
    .withUnlinkedDependencies(unlinkedContainer2a)
  val unlinkedContainer4 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer4"))
    .withUnlinkedDependencies(unlinkedContainer3)
  val unlinkedContainer5 = DockerContainer("nginx:1.7.11", name = Some("unlinkedContainer5"))
  val unlinkedContainers = List(unlinkedContainer1,
                                unlinkedContainer2a,
                                unlinkedContainer2b,
                                unlinkedContainer3,
                                unlinkedContainer4,
                                unlinkedContainer5)

  test("a list of containers with links - build a dependency graph from a list of containers") {
    assertEquals(buildDependencyGraph(unlinkedContainers), ContainerDependencyGraph(
      containers = Seq(unlinkedContainer1, unlinkedContainer5),
      dependants = Some(
        ContainerDependencyGraph(
          containers = Seq(unlinkedContainer2a, unlinkedContainer2b),
          dependants = Some(
            ContainerDependencyGraph(
              containers = Seq(unlinkedContainer3),
              dependants = Some(ContainerDependencyGraph(
                containers = Seq(unlinkedContainer4)
              ))
            ))
        ))
    ))
  }


  test("a list of containers with links - build the dep graph from an empty list of containers") {
    assertEquals(buildDependencyGraph(Seq.empty), ContainerDependencyGraph(
      containers = Seq.empty,
      dependants = None
    ))
  }

  test("a list of containers with links - init all containers taking into account their deps") {
    val dockerKit = new DockerKit with DockerKitDockerJava {
      override def dockerContainers = unlinkedContainers ++ super.dockerContainers
    }
    dockerKit.startAllOrFail()
    dockerKit.stopAllQuietly()
  }
}
