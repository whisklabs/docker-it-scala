package com.whisk.docker

trait DockerRedisSentinelService extends DockerKit {

  val DefaultRedisPort = 26379

  val redisSentinelContainer = DockerContainer("joshula/redis-sentinel")
    .withPorts(DefaultRedisPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("monitor master mymaster"))

  abstract override def dockerContainers = redisSentinelContainer :: super.dockerContainers
}
