package com.whisk.docker.impl.spotify

import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.{DockerFactory, DockerKit}

trait DockerKitSpotify extends DockerKit {

  override implicit val dockerFactory: DockerFactory = new SpotifyDockerFactory(
      DefaultDockerClient.fromEnv().build())
}
