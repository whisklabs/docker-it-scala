package com.whisk.docker.specs2

import com.whisk.docker.DockerKit
import org.slf4j.LoggerFactory

trait DockerTestKit extends BeforeAfterAllStopOnError with DockerKit {
  private lazy val log = LoggerFactory.getLogger(this.getClass)

  def beforeAll(): Unit = {
    startAllOrFail()
  }

  def afterAll(): Unit = {
    stopAllQuietly()
  }
}
