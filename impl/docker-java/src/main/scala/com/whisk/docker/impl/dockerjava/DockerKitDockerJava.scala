package com.whisk.docker.impl.dockerjava

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.whisk.docker.{DockerFactory, DockerKit}

trait DockerKitDockerJava extends DockerKit {

  override implicit val dockerFactory: DockerFactory = new DockerJavaExecutorFactory(
    new Docker(DefaultDockerClientConfig.createDefaultConfigBuilder().build()))
}
