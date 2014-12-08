package whisk.docker

import com.spotify.docker.client.DefaultDockerClient

class Docker {
  val builder = DefaultDockerClient.fromEnv()

  val client = builder.build()

  val host = builder.uri().getHost
}
