package whisk.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{ DockerClientBuilder, DockerClientConfig => DCC }

trait DockerClientKit extends DockerClientConfig {
  implicit override val dockerClient: DockerClient = {
    val dockerConfig = DCC.createDefaultConfigBuilder()
    DockerClientBuilder.getInstance(dockerConfig).build()
  }
}
