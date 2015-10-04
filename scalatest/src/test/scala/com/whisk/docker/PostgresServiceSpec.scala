package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Second, Seconds, Span }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }

class PostgresServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures
    with DockerTestKit
    with DockerPostgresService {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "postgres node" should "be ready with log line checker" in {
    postgresContainer.isReady().futureValue shouldBe true
  }
}
