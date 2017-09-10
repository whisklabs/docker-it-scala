//package com.whisk.docker
//
//trait DockerMongodbService extends DockerKit {
//
//  val DefaultMongodbPort = 27017
//
//  val mongodbContainer = ContainerSpec("mongo:3.0.6")
//    .withPorts(DefaultMongodbPort -> None)
//    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
//    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")
//
//  abstract override def dockerContainers: List[ContainerSpec] =
//    mongodbContainer :: super.dockerContainers
//}
