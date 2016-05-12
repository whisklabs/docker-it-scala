package com.whisk.docker

import com.github.dockerjava.api.command.DockerCmdExecFactory
import com.github.dockerjava.core.{DockerClientBuilder, DockerClientConfig}
import com.github.dockerjava.netty.DockerCmdExecFactoryImpl

class Docker(val config: DockerClientConfig, factory: DockerCmdExecFactory = new DockerCmdExecFactoryImpl) {
  val client = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(factory).build()
  val host = config.getDockerHost.getHost
}
