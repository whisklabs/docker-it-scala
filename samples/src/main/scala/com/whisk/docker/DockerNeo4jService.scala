//package com.whisk.docker
//
//import scala.concurrent.duration._
//
//trait DockerNeo4jService extends DockerKit {
//
//  val DefaultNeo4jHttpPort = 7474
//
//  val neo4jContainer = ContainerSpec("neo4j:3.0.3")
//    .withPorts(DefaultNeo4jHttpPort -> None)
//    .withEnv("NEO4J_AUTH=none")
//    .withReadyChecker(
//      DockerReadyChecker
//        .HttpResponseCode(DefaultNeo4jHttpPort, "/db/data/")
//        .within(100.millis)
//        .looped(20, 1250.millis)
//    )
//
//  abstract override def dockerContainers: List[ContainerSpec] =
//    neo4jContainer :: super.dockerContainers
//}
