package whisk.docker

import org.scalatest.{ BeforeAndAfterAll, Suite }

import scala.concurrent.ExecutionContext

trait DockerTestKit extends BeforeAndAfterAll {
  self: Suite with DockerServiceSetting with DockerClientConfig =>

  // we need ExecutionContext in order to run docker.init() / docker.stop() there
  implicit def executionContext: ExecutionContext = ExecutionContext.global

  override def beforeAll(): Unit = {
    super.beforeAll()
    docker.init()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    docker.stop()
  }
}

