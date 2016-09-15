package com.whisk.docker

import java.net.InetAddress

import com.github.dockerjava.api.command.DockerCmdExecFactory
import com.github.dockerjava.core.{DockerClientBuilder, DockerClientConfig}
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory

class Docker(val config: DockerClientConfig,
            protected val factory: DockerCmdExecFactory = new JerseyDockerCmdExecFactory) {
  val client = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(factory).build()
  val host = Option(config.getDockerHost.getHost).getOrElse(InetAddress.getLoopbackAddress.getHostAddress)
}
