package com.whisk.docker

import scala.concurrent.duration._

case class DockerTestTimeouts(
    pull: FiniteDuration = 5.minutes,
    init: FiniteDuration = 60.seconds,
    stop: FiniteDuration = 10.seconds
)

object DockerTestTimeouts {

  val Default = DockerTestTimeouts()
}
