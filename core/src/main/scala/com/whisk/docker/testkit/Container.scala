package com.whisk.docker.testkit

import java.util.concurrent.atomic.AtomicReference

import com.spotify.docker.client.messages.ContainerInfo
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

sealed trait ContainerState

object ContainerState {

  trait HasId extends ContainerState {
    val id: String
  }

  trait IsRunning extends HasId {
    val info: ContainerInfo
    override val id: String = info.id
  }

  object NotStarted extends ContainerState
  case class Created(id: String) extends ContainerState with HasId
  case class Starting(id: String) extends ContainerState with HasId
  case class Running(info: ContainerInfo) extends ContainerState with IsRunning
  case class Ready(info: ContainerInfo) extends ContainerState with IsRunning
  case class Failed(id: String) extends ContainerState
  object Stopped extends ContainerState
}

class Container(val spec: ContainerSpec) {

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  private val _state = new AtomicReference[ContainerState](ContainerState.NotStarted)

  def state(): ContainerState = {
    _state.get()
  }

  private def updateState(state: ContainerState): Unit = {
    _state.set(state)
  }

  private[docker] def created(id: String): Unit = {
    updateState(ContainerState.Created(id))
  }

  private[docker] def starting(id: String): Unit = {
    updateState(ContainerState.Starting(id))
  }

  private[docker] def running(info: ContainerInfo): Unit = {
    updateState(ContainerState.Running(info))
  }

  private[docker] def ready(info: ContainerInfo): Unit = {
    updateState(ContainerState.Ready(info))
  }

  private def addresses(info: ContainerInfo): Seq[String] = {
    val addrs: Iterable[String] = for {
      networks <- Option(info.networkSettings().networks()).map(_.asScala).toSeq
      (key, network) <- networks
      ip <- Option(network.ipAddress)
    } yield {
      ip
    }
    addrs.toList
  }

  private def portsFrom(info: ContainerInfo): Map[Int, Int] = {
    info
      .networkSettings()
      .ports()
      .asScala
      .collect {
        case (portStr, bindings) if Option(bindings).exists(!_.isEmpty) =>
          val port = ContainerPort.parsed(portStr).port
          val hostPort = bindings.get(0).hostPort().toInt
          port -> hostPort
      }
      .toMap
  }

  def ipAddresses(): Seq[String] = {
    state() match {
      case s: ContainerState.IsRunning =>
        addresses(s.info)
      case _ =>
        throw new Exception("can't get addresses of not running container")
    }
  }

  def mappedPorts(): Map[Int, Int] = {
    state() match {
      case s: ContainerState.IsRunning =>
        portsFrom(s.info)
      case _ =>
        throw new Exception("can't get ports of not running container")
    }
  }

  def toManagedContainer: SingleContainer = SingleContainer(this)
}
