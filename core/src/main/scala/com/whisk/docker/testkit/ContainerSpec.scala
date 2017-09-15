package com.whisk.docker.testkit

import com.spotify.docker.client.messages.PortBinding

case class ContainerSpec(image: String,
                         name: Option[String] = None,
                         command: Option[Seq[String]] = None,
                         portBindings: Map[Int, PortBinding] = Map.empty,
                         env: Seq[String] = Seq.empty,
                         readyChecker: Option[DockerReadyChecker] = None) {

  def withCommand(cmd: String*): ContainerSpec = copy(command = Some(cmd))

  def withExposedPorts(ports: Int*): ContainerSpec = {
    val binds: Map[Int, PortBinding] =
      ports.map(p => p -> PortBinding.randomPort("0.0.0.0"))(collection.breakOut) //TODO check
    copy(portBindings = binds)
  }

  def withPortBindings(ps: (Int, PortBinding)*): ContainerSpec = copy(portBindings = ps.toMap)

  def withReadyChecker(checker: DockerReadyChecker): ContainerSpec =
    copy(readyChecker = Some(checker))

  def withEnv(env: String*): ContainerSpec = copy(env = env)

  def toContainer: Container = new Container(this)

  def toManagedContainer: SingleContainer = SingleContainer(this.toContainer)
}
