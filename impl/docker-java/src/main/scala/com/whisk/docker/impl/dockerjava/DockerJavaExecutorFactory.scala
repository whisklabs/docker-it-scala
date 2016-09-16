package com.whisk.docker.impl.dockerjava

import com.whisk.docker.{DockerCommandExecutor, DockerFactory}

class DockerJavaExecutorFactory(docker: Docker) extends DockerFactory {
  override def createExecutor(): DockerCommandExecutor = {
    new DockerJavaExecutor(docker.host, docker.client)
  }
}
