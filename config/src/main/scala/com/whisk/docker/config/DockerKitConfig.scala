package com.whisk.docker.config

import com.typesafe.config.ConfigFactory
import com.whisk.docker.{DockerContainer, DockerKit}
import com.whisk.docker.config.DockerTypesafeConfig._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

trait DockerKitConfig extends DockerKit {
  def dockerConfig = ConfigFactory.load()

  def configureDockerContainer(configurationName: String): DockerContainer = {
    dockerConfig.as[DockerConfig](configurationName).toDockerContainer
  }
}
