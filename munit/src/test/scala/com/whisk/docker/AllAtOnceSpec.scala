package com.whisk.docker

import com.whisk.docker.impl.spotify.DockerKitSpotify
import _root_.munit.FunSuite

class AllAtOnceSpec
    extends FunSuite
    with DockerKitSpotify
    with DockerElasticsearchService
    with DockerCassandraService
    with DockerNeo4jService
    with DockerMongodbService
    with PingContainerKit {

  test("all containers should be ready at the same time") {
    dockerContainers.map(_.image).foreach(println)
    dockerContainers.foreach(c => isContainerReady(c).map(assert(_)))
  }
}
