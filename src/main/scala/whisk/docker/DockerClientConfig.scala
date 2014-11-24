package whisk.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{ DockerClientBuilder, DockerClientConfig => DCC }

trait DockerClientConfig {
  implicit val dockerClient: DockerClient = {
    val dockerConfig = DCC.createDefaultConfigBuilder()
    DockerClientBuilder.getInstance(dockerConfig).build()
  }
}
