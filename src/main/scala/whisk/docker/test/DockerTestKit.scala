package whisk.docker.test

import java.io.ByteArrayInputStream
import java.util.logging.LogManager

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{ BeforeAndAfterAll, Suite }
import org.slf4j.LoggerFactory
import whisk.docker.DockerKit

trait DockerTestKit extends BeforeAndAfterAll with ScalaFutures with DockerKit {
  self: Suite =>

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  {
    // TODO: logging is required: there could be strange behaviours related to docker init/stop process, several containers interfering each other, etc.
    val lm = LogManager.getLogManager
    lm.reset()
    val lmConfig =
      """handlers = java.util.logging.ConsoleHandler
        |.level = ALL
        |java.util.logging.ConsoleHandler.level = ALL
        |java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
        |""".stripMargin

    lm.readConfiguration(new ByteArrayInputStream(lmConfig.getBytes))
  }

  def dockerInitPatienceInterval = PatienceConfig(scaled(Span(20, Seconds)), scaled(Span(10, Millis)))

  def dockerPullImagesPatienceInterval = PatienceConfig(scaled(Span(1200, Seconds)), scaled(Span(250, Millis)))

  override def beforeAll(): Unit = {
    super.beforeAll()

    super.pullImages().futureValue(dockerPullImagesPatienceInterval)

    val allRunning = try {
      super
        .initReadyAll()
        .map(
          _.map(_._2)
            .forall(identity)
        ).recover {
            case e =>
              logException(e)
              log.error("Cannot run docker containers", e)
              false
          }
        .futureValue(dockerInitPatienceInterval)
    } catch {
      case e: Exception =>
        logException(e)
        false
    }

    if (!allRunning) {
      stopRmAll().futureValue(dockerInitPatienceInterval)
      throw new RuntimeException("Cannot run all required containers")
    }
  }

  override def afterAll(): Unit = {
    try {
      // We should wait, and we should catch.
      // Otherwise there's a lot of java-style strangeness when you run several tests with a lot of containers (not with sbt test-only, but with sbt test) or when an exception is thrown
      // Anyway with current PatienceConfig it's fast
      stopRmAll().futureValue(dockerInitPatienceInterval)
    } catch {
      case e: Throwable =>
        logException(e)
        throw e
    }

    super.afterAll()

  }
}

