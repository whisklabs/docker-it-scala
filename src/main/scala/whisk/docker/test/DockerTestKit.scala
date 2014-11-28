package whisk.docker.test

import org.scalatest.concurrent.{ PatienceConfiguration, ScalaFutures }
import org.scalatest.time._
import org.scalatest.{ BeforeAndAfterAll, Suite }
import org.slf4j.LoggerFactory
import whisk.docker.{ DockerKit, DockerConfig, DockerContainer }

trait DockerTestKit extends BeforeAndAfterAll with ScalaFutures with DockerKit {
  self: Suite with DockerConfig =>

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  def dockerInitPatienceInterval = PatienceConfiguration.Interval(Span(20, Seconds))

  override def beforeAll(): Unit = {
    super.beforeAll()

    val allRunning = super
      .initReadyAll()
      .map(
        _.map(_._2)
          .forall(identity)
      ).recover {
          case e =>
            log.error("Cannot run docker containers", e)
            false
        }
      .futureValue(dockerInitPatienceInterval)

    if (!allRunning) {
      stopRmAll().futureValue(dockerInitPatienceInterval)
      throw new RuntimeException("Cannot run all required containers")
    }
  }

  override def afterAll(): Unit = {
    stopRmAll()
    super.afterAll()
  }
}

