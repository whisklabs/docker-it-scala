package com.whisk.docker.munit

import com.whisk.docker.DockerKit
import org.slf4j.LoggerFactory

trait DockerTestKit extends DockerKit { self: munit.FunSuite =>
  private lazy val log = LoggerFactory.getLogger(this.getClass)

  override def beforeAll(): Unit = {
    startAllOrFail()
  }

  override def afterAll(): Unit = {
    stopAllQuietly()
  }
}
