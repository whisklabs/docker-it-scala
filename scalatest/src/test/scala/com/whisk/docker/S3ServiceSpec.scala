package com.whisk.docker

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._

class S3ServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerS3Service with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "s3 container" should "be ready" in {
    s3Container.isReady().futureValue shouldBe true
  }
}
