package com.whisk.docker

trait DockerZookeeperService extends DockerKit {

  val zookeeperContainer = DockerContainer("jplock/zookeeper:3.4.6")
    .withPorts(2181 -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("binding to port"))

  abstract override def dockerContainers: List[DockerContainer] =
    zookeeperContainer :: super.dockerContainers
}
