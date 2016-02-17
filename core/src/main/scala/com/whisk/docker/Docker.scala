package com.whisk.docker

import com.github.dockerjava.core.{DockerClientBuilder, DockerClientConfig}
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl

class Docker(val config: DockerClientConfig) {
  val client = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(new DockerCmdExecFactoryImpl).build()
  val host = config.getDockerHost.getHost
}
