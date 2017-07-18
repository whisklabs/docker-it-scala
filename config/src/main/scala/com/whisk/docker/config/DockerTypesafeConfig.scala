package com.whisk.docker.config

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.{DockerContainer, DockerPortMapping, DockerReadyChecker, HostConfig, VolumeMapping}

import scala.concurrent.duration._

object DockerTypesafeConfig extends DockerKitDockerJava {
  val EmptyPortBindings: Map[Int, Option[Int]] = Map.empty
  val AlwaysReady = DockerReadyChecker.Always

  case class DockerConfigPortMap(internal: Int, external: Option[Int]) {
    def asTuple = (internal, external)
  }

  case class DockerConfigReadyCheckerLooped(attempts: Int, delay: Int)

  case class DockerConfigHttpResponseReady(port: Int,
                                           path: String = "/",
                                           host: Option[String],
                                           code: Int = 200,
                                           within: Option[Int],
                                           looped: Option[DockerConfigReadyCheckerLooped])

  case class DockerConfigReadyChecker(
      `log-line`: Option[String],
      `http-response-code`: Option[DockerConfigHttpResponseReady]) {

    def httpResponseCodeReadyChecker(rr: DockerConfigHttpResponseReady) = {
      val codeChecker: DockerReadyChecker =
        DockerReadyChecker.HttpResponseCode(rr.port, rr.path, rr.host, rr.code)
      val within = rr.within.fold(codeChecker)(w => codeChecker.within(w.millis))
      rr.looped.fold(within)(l => within.looped(l.attempts, l.delay.millis))
    }

    // log line checker takes priority
    def toReadyChecker = {
      (`log-line`, `http-response-code`) match {
        case (None, None)     => DockerReadyChecker.Always
        case (None, Some(rr)) => httpResponseCodeReadyChecker(rr)
        case (Some(ll), _)    => DockerReadyChecker.LogLineContains(ll)
      }
    }
  }

  case class DockerConfig(`image-name`: String,
                          `container-name`: Option[String],
                          command: Option[Seq[String]],
                          entrypoint: Option[Seq[String]],
                          `environmental-variables`: Seq[String] = Seq.empty,
                          `port-maps`: Option[Map[String, DockerConfigPortMap]],
                          `ready-checker`: Option[DockerConfigReadyChecker],
                          `volume-maps`: Seq[VolumeMapping] = Seq.empty,
                          memory: Option[Long],
                          `memory-reservation`: Option[Long]) {

    def toDockerContainer(): DockerContainer = {
      val bindPorts = `port-maps`.fold(EmptyPortBindings) { _.values.map(_.asTuple).toMap } mapValues {
        maybeHostPort =>
          DockerPortMapping(maybeHostPort)
      }

      val readyChecker = `ready-checker`.fold[DockerReadyChecker](AlwaysReady) { _.toReadyChecker }

      val hostConfig = HostConfig(
        memory = memory,
        memoryReservation = `memory-reservation`
      )

      DockerContainer(
        image = `image-name`,
        name = `container-name`,
        command = command,
        entrypoint = entrypoint,
        bindPorts = bindPorts,
        env = `environmental-variables`,
        readyChecker = readyChecker,
        volumeMappings = `volume-maps`,
        hostConfig = Some(hostConfig)
      )
    }
  }
}
