package com.whisk.docker

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.whisk.docker.impl.dockerjava.{Docker, DockerJavaExecutorFactory}
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

class CassandraServiceSpec
    extends FlatSpec
    with Matchers
    with DockerCassandraService
    with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  override implicit val dockerFactory: DockerFactory = new DockerJavaExecutorFactory(
      new Docker(DefaultDockerClientConfig.createDefaultConfigBuilder().build(),
                 factory = new NettyDockerCmdExecFactory()))

  "cassandra node" should "be ready with log line checker" in {
    isContainerReady(cassandraContainer).futureValue shouldBe true
  }
}
