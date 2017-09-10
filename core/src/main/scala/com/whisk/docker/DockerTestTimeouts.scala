package com.whisk.docker

import scala.concurrent.duration._

case class DockerTestTimeouts(
    init: FiniteDuration = 60.seconds,
    stop: FiniteDuration = 10.seconds
)

object DockerTestTimeouts {

  val Default = DockerTestTimeouts()
}
