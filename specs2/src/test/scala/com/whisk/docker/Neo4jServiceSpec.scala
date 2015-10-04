package com.whisk.docker

import com.whisk.docker.specs2.DockerTestKit
import org.specs2._
import org.specs2.specification.core.Env
import scala.concurrent._
import scala.concurrent.duration._

class Neo4jServiceSpec(env: Env) extends Specification
    with DockerTestKit
    with DockerNeo4jService {

  implicit val ee = env.executionEnv
  implicit val ec = env.executionContext

  def is = s2"""
  The neo4j container should
    be ready                     $x1
    show it's logs               $x2
    pass ready checker with logs $x3
                                 """

  def x1 = neo4jContainer.isReady() must beTrue.await

  def x2 = {
    val neo4jId = Await.result(neo4jContainer.id, 1.seconds)
    val is = docker.client.logContainerCmd(neo4jId).withStdOut().exec()

    def pullLines(it: Iterator[String], num: Int): List[String] = num match {
      case 0 => Nil
      case _ if !it.hasNext => Nil
      case n =>
        it.next() :: pullLines(it, n - 1)
    }

    val src = scala.io.Source.fromInputStream(is)(scala.io.Codec.ISO8859)

    val lns = pullLines(src.getLines(), 10)

    println(lns)

    lns must have size(10)
  }

  def x3 = {
    val c = DockerReadyChecker.LogLine(_.contains("Starting HTTP on port :7474"))
    c(neo4jContainer) must beTrue.await
  }
}
