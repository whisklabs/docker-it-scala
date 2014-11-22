package whisk.docker

import org.scalatest.{ BeforeAndAfterAll, Suite }

import scala.concurrent.ExecutionContext

trait DockerTestKit extends BeforeAndAfterAll {
  self: Suite with DockerClientConfig =>

  // we need ExecutionContext in order to run docker.init() / docker.stop() there
  implicit def dockerExecutionContext: ExecutionContext = ExecutionContext.global

  def dockerContainers: List[DockerContainer] = Nil

  override def beforeAll(): Unit = {
    super.beforeAll()
    dockerContainers.foreach(_.init())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    dockerContainers.foreach(_.stop())
  }
}

