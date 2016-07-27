package com.whisk.docker

class DockerJavaExecutorFactory(docker: Docker) extends DockerFactory {
  override def createExecutor(): DockerCommandExecutor = {
    new DockerJavaExecutor(docker.host, docker.client)
  }
}
