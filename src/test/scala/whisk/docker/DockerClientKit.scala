package whisk.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{ DockerClientBuilder, DockerClientConfig => DCC }

trait DockerClientKit extends DockerClientConfig {
  implicit override val dockerClient: DockerClient = {
    val dockerConfig = DCC.createDefaultConfigBuilder()
      .withUri("https://192.168.59.103:2376")
      .withDockerCertPath("/Users/alari/.boot2docker/certs/boot2docker-vm")
      .build()
    DockerClientBuilder.getInstance(dockerConfig).build()
  }
}
