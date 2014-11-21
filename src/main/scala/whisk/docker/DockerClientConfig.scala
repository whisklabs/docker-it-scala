package whisk.docker

import com.github.dockerjava.api.DockerClient

trait DockerClientConfig {
  implicit def dockerClient: DockerClient
}
