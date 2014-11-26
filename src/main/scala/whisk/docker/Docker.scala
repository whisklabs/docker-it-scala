package whisk.docker

import com.github.dockerjava.core.{ DockerClientBuilder, DockerClientConfig => DCC }

class Docker(val config: DCC) {
  lazy val client = DockerClientBuilder.getInstance(config).build()
}
