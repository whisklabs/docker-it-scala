package com.whisk.docker

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, GivenWhenThen, Matchers}

import scala.concurrent.Future

class AllAtOnceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerElasticsearchService with DockerCassandraService with DockerNeo4jService with DockerMongodbService with PingContainerKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "all containers" should "be ready at the same time" in {
    dockerContainers.map(_.image).foreach(println)

    // Merge the separate futures to one, ready when all the components are
    //
    // Note: This is slightly different than the preceding code, which would fail immediately,
    //      if some container's '.isReady' gives false. Here, all the futures are waited for,
    //      before making the check. Likely doesn't matter in practise. AKa180216
    //
    val allFut: Future[Seq[Boolean]] = Future.sequence( dockerContainers.map(_.isReady) )

    whenReady(allFut) { xs =>
      every (xs) shouldBe true
    }
  }
}
