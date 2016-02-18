package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Second, Seconds, Span }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, GivenWhenThen, Matchers }

class CassandraServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerCassandraService with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "cassandra node" should "be ready with log line checker" in {

    whenReady(cassandraContainer.isReady) { _ shouldBe true }
  }
}
