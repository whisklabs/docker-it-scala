package com.whisk.docker.impl.spotify

import com.spotify.docker.client.DockerClient
import com.whisk.docker.{DockerCommandExecutor, DockerFactory}

class SpotifyDockerFactory(client: DockerClient) extends DockerFactory {

  override def createExecutor(): DockerCommandExecutor = {
    new SpotifyDockerCommandExecutor(client.getHost, client)
  }
}
