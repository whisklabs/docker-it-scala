package whisk.docker.test

import java.io.ByteArrayInputStream
import java.util.logging.LogManager

import com.spotify.docker.client.DockerClient.LogsParameter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{ BeforeAndAfterAll, Suite }
import org.slf4j.LoggerFactory
import whisk.docker.DockerKit

import scala.concurrent.Future

trait DockerTestKit extends BeforeAndAfterAll with ScalaFutures with DockerKit {
  self: Suite =>

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  {
    val lm = LogManager.getLogManager
    lm.reset()
    val lmConfig =
      """handlers = java.util.logging.ConsoleHandler
      |.level = OFF
      |java.util.logging.ConsoleHandler.level = OFF
      |java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
      |"""
        .stripMargin

    lm.readConfiguration(new ByteArrayInputStream(lmConfig.getBytes))
  }

  def dockerInitPatienceInterval = PatienceConfig(scaled(Span(20, Seconds)), scaled(Span(10, Millis)))

  override def beforeAll(): Unit = {
    super.beforeAll()

    val allRunning = super
      .initReadyAll()
      .map {
        _.map {
          case (container, false) =>
            for {
              id <- container.id
              is <- Future(docker.client.logs(id, LogsParameter.FOLLOW, LogsParameter.STDERR, LogsParameter.FOLLOW))
            } {
              log.error(is.readFully())
            }
            log.error(s"Failed to run: ${container.image}")
            false
          case (_, true) => true
        }
          .forall(identity)
      }.recover {
        case e =>
          e.printStackTrace()
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

