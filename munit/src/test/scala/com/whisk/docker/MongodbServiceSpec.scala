package com.whisk.docker

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.munit.DockerTestKit
import _root_.munit.FunSuite

class MongodbServiceSpec
    extends FunSuite
    with DockerTestKit
    with DockerKitSpotify
    with DockerMongodbService {

  test("mongodb node should be ready with log line checker") {
    isContainerReady(mongodbContainer).map(assert(_))
    mongodbContainer.getPorts().map(m => assert(m.get(27017).nonEmpty))
    mongodbContainer.getIpAddresses().map(s => assert(s.nonEmpty))
  }
}
