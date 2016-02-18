package com.whisk.docker

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, GivenWhenThen, Matchers}

import scala.concurrent.Future

class AllAtOnceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerElasticsearchService with DockerCassandraService with DockerNeo4jService with DockerMongodbService with PingContainerKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "all containers" should "be ready" in {
    dockerContainers.map(_.image).foreach(println)
    dockerContainers.forall(_.isReady.futureValue) shouldBe true

    /*
    * Note: Above could be forged into a single 'Future[Seq[Boolean]]' but in that case,
    *       a failing future would not terminate the test immediately, as it probably does
    *       above. So here, '.futureValue' is probably the right way to go? AKa180216
    *
    whenReady( Future.sequence( dockerContainers.map(_.isReady) ) ) { xs =>
      every (xs) shouldBe true
    }
    */
  }
}
