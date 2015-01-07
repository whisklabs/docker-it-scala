package whisk.docker

import com.github.dockerjava.core.{ DockerClientConfig, DockerClientBuilder }
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl

class Docker(val config: DockerClientConfig) {
  val client = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(new DockerCmdExecFactoryImpl).build()
  val host = config.getUri.getHost
}
