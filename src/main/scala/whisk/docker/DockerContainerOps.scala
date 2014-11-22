package whisk.docker

import com.github.dockerjava.api.DockerClient

import scala.concurrent.ExecutionContext

trait DockerContainerOps {
  self: DockerContainer =>

  def stop()(implicit adapter: DockerOps, dockerClient: DockerClient, ec: ExecutionContext) = adapter.stop(self :: Nil)

}
