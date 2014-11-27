package whisk.docker.test

import org.scalatest.concurrent.{ PatienceConfiguration, ScalaFutures }
import org.scalatest.time._
import org.scalatest.{ BeforeAndAfterAll, Suite }
import whisk.docker.{ DockerKit, DockerConfig, DockerContainer }

trait DockerTestKit extends BeforeAndAfterAll with ScalaFutures with DockerKit {
  self: Suite with DockerConfig =>

  def dockerInitPatienceInterval = PatienceConfiguration.Interval(Span(20, Seconds))

  private def stopRemoveAll(): Seq[DockerContainer] = {
    super.stopRmAll().futureValue(dockerInitPatienceInterval)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    val allRunning = super
      .initReadyAll()
      .map(
        _.map(_._2)
          .forall(identity)
      ).recover { case _ => false }
      .futureValue(dockerInitPatienceInterval)

    if (!allRunning) {
      stopRemoveAll()
      throw new RuntimeException("Cannot run all required containers")
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopRemoveAll()
  }
}

