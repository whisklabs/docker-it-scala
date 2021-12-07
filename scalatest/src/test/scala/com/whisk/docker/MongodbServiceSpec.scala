package com.whisk.docker

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration

class MongodbServiceSpec
    extends AnyFlatSpec
    with Matchers
    with DockerTestKit
    with DockerKitSpotify
    with DockerMongodbService {

  implicit val pc: PatienceConfig = PatienceConfig(Span(20, Seconds), Span(1, Second))
  implicit val defaultOpsTimeout: Duration = Duration.Inf

  "mongodb node" should "be ready with log line checker" in {
    isContainerReady(mongodbContainer).futureValue shouldBe true
    mongodbContainer.getPorts().futureValue.get(27017) should not be empty
    mongodbContainer.getIpAddresses().futureValue should not be Seq.empty
  }
}
