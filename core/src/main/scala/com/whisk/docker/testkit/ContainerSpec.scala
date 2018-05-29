package com.whisk.docker.testkit

import java.util.Collections

import com.spotify.docker.client.messages.{ContainerConfig, HostConfig, PortBinding}
import com.spotify.docker.client.messages.HostConfig.Bind

import scala.collection.JavaConverters._

case class ContainerSpec(image: String) {

  private val builder: ContainerConfig.Builder = ContainerConfig.builder().image(image)
  private val hostConfigBuilder: HostConfig.Builder = HostConfig.builder()

  private var _readyChecker: Option[DockerReadyChecker] = None
  private var _name: Option[String] = None

  def withCommand(cmd: String*): ContainerSpec = {
    builder.cmd(cmd: _*)
    this
  }

  def withExposedPorts(ports: Int*): ContainerSpec = {
    val binds: Seq[(Int, PortBinding)] =
      ports.map(p => p -> PortBinding.randomPort("0.0.0.0"))(collection.breakOut)
    withPortBindings(binds: _*)
  }

  def withPortBindings(ps: (Int, PortBinding)*): ContainerSpec = {
    val binds: Map[String, java.util.List[PortBinding]] = ps.map {
      case (guestPort, binding) =>
        guestPort.toString -> Collections.singletonList(binding)
    }(collection.breakOut)

    hostConfigBuilder.portBindings(binds.asJava)
    builder.exposedPorts(binds.keySet.asJava)
    this
  }

  def withVolumeBindings(vs: Bind*): ContainerSpec = {
    hostConfigBuilder.binds(vs: _*)
    this
  }

  def withReadyChecker(checker: DockerReadyChecker): ContainerSpec = {
    _readyChecker = Some(checker)
    this
  }

  def withName(name: String): ContainerSpec = {
    _name = Some(name)
    this
  }

  def withEnv(env: String*): ContainerSpec = {
    builder.env(env: _*)
    this
  }

  def withConfiguration(
      withBuilder: ContainerConfig.Builder => ContainerConfig.Builder
  ): ContainerSpec = {
    withBuilder(builder)
    this
  }

  def withHostConfiguration(
      withBuilder: HostConfig.Builder => HostConfig.Builder
  ): ContainerSpec = {
    withBuilder(hostConfigBuilder)
    this
  }

  def name: Option[String] = _name

  def readyChecker: Option[DockerReadyChecker] = _readyChecker

  def containerConfig(): ContainerConfig = {
    builder.hostConfig(hostConfigBuilder.build()).build()
  }

  def toContainer: Container = new Container(this)

  def toManagedContainer: SingleContainer = SingleContainer(this.toContainer)
}
