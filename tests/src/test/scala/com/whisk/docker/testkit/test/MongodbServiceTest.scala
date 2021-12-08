package com.whisk.docker.testkit.test

import com.whisk.docker.testkit.{ContainerState, DockerMongodbService}
import org.scalatest.funsuite.AnyFunSuite

class MongodbServiceTest extends AnyFunSuite with DockerMongodbService {

  test("test container started") {
    assert(mongodbContainer.state().isInstanceOf[ContainerState.Ready], "mongodb is ready")
    assert(mongodbContainer.mappedPortOpt(27017).nonEmpty, "port 2017 is exposed")
  }
}
