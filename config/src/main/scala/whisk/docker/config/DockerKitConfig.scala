package whisk.docker.config

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.Ficus._

import whisk.docker.{ DockerContainer, DockerKit }
import DockerTypesafeConfig._

trait DockerKitConfig extends DockerKit {
  def dockerConfig = ConfigFactory.load()

  def configureDockerContainer(configurationName: String): DockerContainer = {
    dockerConfig.as[DockerConfig](configurationName).toDockerContainer
  }
}
