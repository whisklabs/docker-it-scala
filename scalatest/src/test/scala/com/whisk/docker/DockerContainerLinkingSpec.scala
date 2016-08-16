package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class DockerContainerLinkingSpec extends FlatSpec with Matchers with DockerTestKit {
     
    val cmdExecutor = implicitly[DockerCommandExecutor]
    implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

    val pingName = "ping"
    val pongName = "pong"
    val pingAlias = "pang"

    val pingService = new PingService(pingName, None)
    val pongService = new PingService(pongName, Some(ContainerLink(pingService.container, pingAlias)))
 
  "A DockerContainer" should "be linked to the specified containers upon start" in {
      pingService.startAllOrFail()
      pongService.startAllOrFail()

      val ping = cmdExecutor.inspectContainer(pingName)
      val pongPing = cmdExecutor.inspectContainer(s"$pongName/$pingAlias")

      whenReady(ping) { pingState =>
        whenReady(pongPing) { pongPingState =>
          pingState should not be empty
          pingState shouldBe pongPingState
        }
      }

      pingService.stopAllQuietly()
      pongService.stopAllQuietly()
  }
}

class PingService(name: String, link: Option[ContainerLink]) extends DockerKit {
  private val tmpContainer = DockerContainer("nginx:1.7.11", name = Some(name))
    .withPorts(80 -> None)
    .withReadyChecker(DockerReadyChecker.HttpResponseCode(port = 80, path = "/", host = None, code = 200)) 

  val container = link.fold(tmpContainer)(link => tmpContainer.withLinks(link))

  override def dockerContainers  = container :: super.dockerContainers
}
