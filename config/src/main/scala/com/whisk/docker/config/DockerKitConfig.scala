package com.whisk.docker.config

import scala.collection.JavaConverters._

import com.typesafe.config.{Config, ConfigFactory}
import com.whisk.docker.{DockerContainer, VolumeMapping}
import com.whisk.docker.config.DockerTypesafeConfig._
import com.whisk.docker.impl.dockerjava.DockerKitDockerJava

trait DockerKitConfig extends DockerKitDockerJava {
  def dockerConfig = ConfigFactory.load()

  implicit class RichConfig(c: Config) {
    def getOpt[T](fn: (Config, String) => T, path: String): Option[T] =
      if (c.hasPath(path)) Some(fn(c, path)) else None
  }

  private def toPortMapping(c: Config): DockerConfigPortMap = DockerConfigPortMap(
    c.getInt("internal"),
    c.getOpt(_ getInt _, "external")
  )
  private def toPortMappings(c: Config): Map[String, DockerConfigPortMap] =
    c.entrySet()
      .asScala
      .map { e =>
        val k = e.getKey.split('.').head
        k -> toPortMapping(c.getConfig(k))
      }
      .toMap
  private def toReadyCheckerLooped(c: Config): DockerConfigReadyCheckerLooped =
    DockerConfigReadyCheckerLooped(c.getInt("attempts"), c.getInt("delay"))
  private def toReadyResponse(c: Config): DockerConfigHttpResponseReady =
    DockerConfigHttpResponseReady(
      c.getInt("port"),
      c.getOpt(_ getString _, "path") getOrElse "/",
      c.getOpt(_ getString _, "host"),
      c.getOpt(_ getInt _, "code").getOrElse(200),
      c.getOpt(_ getInt _, "within"),
      c.getOpt((c, s) => toReadyCheckerLooped(c.getConfig(s)), "looped")
    )
  private def toReadyChecker(c: Config): DockerConfigReadyChecker = DockerConfigReadyChecker(
    c.getOpt(_ getString _, "log-line"),
    c.getOpt((c, s) => toReadyResponse(c.getConfig(s)), "http-response-code")
  )
  private def toVolumeMaps(c: Config): VolumeMapping =
    VolumeMapping(c.getString("host"),
                  c.getString("container"),
                  c.getOpt(_ getBoolean _, "rw").getOrElse(false))
  private def toDockerConfig(c: Config): DockerConfig = DockerConfig(
    c.getString("image-name"),
    c.getOpt(_ getString _, "container-name"),
    c.getOpt((c, s) => c.getStringList(s).asScala.toSeq, "command"),
    c.getOpt((c, s) => c.getStringList(s).asScala.toSeq, "entrypoint"),
    c.getOpt((c, s) => c.getStringList(s).asScala.toSeq, "environmental-variables")
      .getOrElse(Nil),
    c.getOpt((c, s) => toPortMappings(c.getConfig(s)), "port-maps"),
    c.getOpt((c, s) => toReadyChecker(c.getConfig(s)), "ready-checker"),
    c.getOpt((c, s) => c.getConfigList(s).asScala.toSeq.map(toVolumeMaps), "volume-maps")
      .getOrElse(Nil),
    c.getOpt(_ getLong _, "memory"),
    c.getOpt(_ getLong _, "memory-reservation")
  )
  def configureDockerContainer(configurationName: String): DockerContainer = {
    val config =
      if (configurationName == "." || configurationName == "") dockerConfig
      else dockerConfig.getConfig(configurationName)
    toDockerConfig(config).toDockerContainer()
  }
}
