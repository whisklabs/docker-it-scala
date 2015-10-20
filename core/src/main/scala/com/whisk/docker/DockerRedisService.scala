package com.whisk.docker

trait DockerRedisService extends DockerKit {

  val DefaultRedisPort = 6379

  val redisContainer = DockerContainer("redis:3.0.5")
    .withPorts(DefaultRedisPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("Server started"))

  abstract override def dockerContainers = redisContainer :: super.dockerContainers
}
