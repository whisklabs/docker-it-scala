package com.whisk.docker

import org.scalatest._
import time._

import impl.dockerjava._
import impl.spotify._
import scalatest.DockerTestKit
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration.Duration

abstract class DockerContainerLinkingSpec extends AnyFlatSpec with Matchers with DockerTestKit {

  lazy val cmdExecutor = implicitly[DockerCommandExecutor]
  implicit val pc: PatienceConfig = PatienceConfig(Span(20, Seconds), Span(1, Second))
  implicit val defaultOpsTimeout: Duration = Duration.Inf

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

class SpotifyDockerContainerLinkingSpec extends DockerContainerLinkingSpec with DockerKitSpotify
class DockerJavaDockerContainerLinkingSpec
    extends DockerContainerLinkingSpec
    with DockerKitDockerJava
