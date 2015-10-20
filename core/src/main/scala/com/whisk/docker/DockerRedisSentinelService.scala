package com.whisk.docker

trait DockerRedisSentinelService extends DockerKit {

  val DefaultRedisPort = 26379

  val redisSentinelContainer = DockerContainer("joshula/redis-sentinel")
    .withPorts(DefaultRedisPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLineContains("monitor master mymaster"))
    .withCommand("--sentinel announce-ip localhost", s"--sentinel announce-port $DefaultRedisPort")

  abstract override def dockerContainers = redisSentinelContainer :: super.dockerContainers
}
