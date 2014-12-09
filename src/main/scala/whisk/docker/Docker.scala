package whisk.docker

import com.github.dockerjava.core.{ DockerClientBuilder, DockerClientConfig }

class Docker(val config: DockerClientConfig) {
  val client = DockerClientBuilder.getInstance(config).build()

  val host = config.getUri.getHost
}
