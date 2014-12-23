package whisk.docker

import com.github.dockerjava.core.{ DockerClientConfig, DockerClientBuilder }

class Docker(val config: DockerClientConfig) {
  val client = DockerClientBuilder.getInstance(config).build()
  val host = config.getUri.getHost
}
