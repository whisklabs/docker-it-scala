package com.whisk.docker

import _root_.munit.FunSuite

import impl.dockerjava._
import impl.spotify._
import munit.DockerTestKit

abstract class DockerContainerLinkingSpec extends FunSuite with DockerTestKit {

  lazy val cmdExecutor = implicitly[DockerCommandExecutor]

  val pingName = "ping"
  val pongName = "pong"
  val pingAlias = "pang"

  val pingService = DockerContainer("nginx:1.7.11", name = Some(pingName))

  val pongService = DockerContainer("nginx:1.7.11", name = Some(pongName))
    .withLinks(ContainerLink(pingService, pingAlias))

  override def dockerContainers = pingService :: pongService :: super.dockerContainers

  test("A DockerContainer should be linked to the specified containers upon start") {
    val ping = cmdExecutor.inspectContainer(pingName)
    val pongPing = cmdExecutor.inspectContainer(s"$pongName/$pingAlias")

    for {
      pingState <- ping
      pongPingState <- pongPing
    } yield {
      assert(pingState.nonEmpty)
      assertEquals(pingState, pongPingState)
    }
  }
}

class SpotifyDockerContainerLinkingSpec extends DockerContainerLinkingSpec with DockerKitSpotify
class DockerJavaDockerContainerLinkingSpec
    extends DockerContainerLinkingSpec
    with DockerKitDockerJava
