package whisk.docker

import com.github.dockerjava.core.{ DockerClientConfig => DCC }

trait DockerConfig {
  implicit val docker: Docker = new Docker(DCC.createDefaultConfigBuilder().build())
}
