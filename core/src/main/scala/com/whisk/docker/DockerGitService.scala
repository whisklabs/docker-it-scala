package com.whisk.docker

import scala.concurrent.duration._

trait DockerGitService extends DockerKit {

  val DefaultGitPort = 9418

  val gitContainer = DockerContainer("bankiru/git-daemon")
    .withPorts(DefaultGitPort -> Some(DefaultGitPort))
    .withReadyChecker(
      DockerReadyChecker
        .LogLineContains("Ready to rumble",true)
        .within(100.millis)
        .looped(20, 1250.millis)
    )

  abstract override def dockerContainers: List[DockerContainer] =
    gitContainer :: super.dockerContainers
}