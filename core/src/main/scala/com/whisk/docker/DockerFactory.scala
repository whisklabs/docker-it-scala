package com.whisk.docker

trait DockerFactory {

  def createExecutor(): DockerCommandExecutor
}
