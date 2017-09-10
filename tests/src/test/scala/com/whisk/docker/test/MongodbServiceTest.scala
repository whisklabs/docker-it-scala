package com.whisk.docker.test

import com.whisk.docker.{ContainerState, DockerMongodbService}
import org.scalatest.FunSuite

class MongodbServiceTest extends FunSuite with DockerMongodbService {

  test("test container started") {
    assert(mongodbContainer.state().isInstanceOf[ContainerState.Ready], "mongodb is ready")
    assert(mongodbContainer.mappedPorts().get(27017).nonEmpty, "port 2017 is exposed")
  }
}
