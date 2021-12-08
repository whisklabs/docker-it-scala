package com.whisk.docker.testkit.scalatest

import java.util.concurrent.ForkJoinPool

import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.testkit._
import org.scalatest.{Args, Status, Suite, SuiteMixin}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

trait DockerTestKitForAll extends SuiteMixin { self: Suite =>

  val dockerClient: DockerClient = DefaultDockerClient.fromEnv().build()

  val dockerExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool())

  val managedContainers: ManagedContainers

  val dockerTestTimeouts: DockerTestTimeouts = DockerTestTimeouts.Default

  implicit lazy val dockerExecutor: ContainerCommandExecutor =
    new ContainerCommandExecutor(dockerClient)

  lazy val containerManager = new DockerContainerManager(
    managedContainers,
    dockerExecutor,
    dockerTestTimeouts,
    dockerExecutionContext
  )

  abstract override def run(testName: Option[String], args: Args): Status = {
    containerManager.start()
    afterStart()
    try {
      super.run(testName, args)
    } finally {
      try {
        beforeStop()
      } finally {
        containerManager.stop()
      }
    }
  }

  def afterStart(): Unit = {}

  def beforeStop(): Unit = {}

}
