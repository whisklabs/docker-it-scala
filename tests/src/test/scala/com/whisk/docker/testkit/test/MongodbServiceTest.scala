package com.whisk.docker.testkit.test

import com.whisk.docker.testkit.{ContainerState, DockerMongodbService}
import org.scalatest.FunSuite

class MongodbServiceTest extends FunSuite with DockerMongodbService {

  test("test container started") {
    assert(mongodbContainer.state().isInstanceOf[ContainerState.Ready], "mongodb is ready")
    assert(mongodbContainer.mappedPorts().get(27017).nonEmpty, "port 2017 is exposed")
  }
}
