package whisk.docker

import com.github.dockerjava.core.{ DockerClientBuilder, DockerClientConfig => DCC }
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl

class Docker(val config: DCC) {
  lazy val client = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(new DockerCmdExecFactoryImpl).build()

  lazy val host = config.getUri.getHost
}
