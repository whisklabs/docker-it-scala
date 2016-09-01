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

    val pingService = DockerContainer("nginx:1.7.11", name = Some(pingName))

    val pongService = DockerContainer("nginx:1.7.11", name = Some(pongName))
      .withLinks(ContainerLink(pingService, pingAlias))

    override def dockerContainers = pingService :: pongService :: super.dockerContainers
 
  "A DockerContainer" should "be linked to the specified containers upon start" in {
    val ping = cmdExecutor.inspectContainer(pingName)
    val pongPing = cmdExecutor.inspectContainer(s"$pongName/$pingAlias")

    whenReady(ping) { pingState =>
      whenReady(pongPing) { pongPingState =>
        pingState should not be empty
        pingState shouldBe pongPingState
      }
    }
  }
}
