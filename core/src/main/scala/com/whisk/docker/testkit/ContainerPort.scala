package com.whisk.docker.testkit

object PortProtocol extends Enumeration {
  val TCP, UDP = Value
}

case class ContainerPort(port: Int, protocol: PortProtocol.Value)

object ContainerPort {
  def parsed(str: String): ContainerPort = {
    val Array(p, rest @ _*) = str.split("/")
    val proto = rest.headOption
      .flatMap(pr => PortProtocol.values.find(_.toString.equalsIgnoreCase(pr)))
      .getOrElse(PortProtocol.TCP)
    ContainerPort(p.toInt, proto)
  }
}