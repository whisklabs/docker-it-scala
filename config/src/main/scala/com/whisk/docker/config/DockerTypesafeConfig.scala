package com.whisk.docker.config

import scala.concurrent.duration._

import com.whisk.docker.{ DockerContainer, DockerKit, DockerReadyChecker }

object DockerTypesafeConfig extends DockerKit {
  val EmptyPortBindings: Map[Int, Option[Int]] = Map.empty
  val AlwaysReady = DockerReadyChecker.Always

  case class DockerConfigPortMap(internal: Int, external: Option[Int]) {
    def asTuple = (internal, external)
  }

  case class DockerConfigReadyCheckerLooped(attempts: Int, delay: Int)

  case class DockerConfigHttpResponseReady (
    port: Int, path: String = "/",
    host: Option[String], code: Int = 200,
    within: Option[Int], looped: Option[DockerConfigReadyCheckerLooped])

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
        case (None, None) => DockerReadyChecker.Always
        case (None, Some(rr)) => httpResponseCodeReadyChecker(rr)
        case (Some(ll), _) => DockerReadyChecker.LogLine(_.contains(ll))
      }
    }
  }

  case class DockerConfig (
    `image-name`: String, command: Option[Seq[String]],
    `environmental-variables`: Seq[String] = Seq.empty,
    `port-maps`: Option[Map[String, DockerConfigPortMap]],
    `ready-checker`: Option[DockerConfigReadyChecker]) {

    def toDockerContainer() = {
      val bindPorts =
        `port-maps`
          .fold(EmptyPortBindings) { _.values.map(_.asTuple).toMap }

      val readyChecker =
        `ready-checker`
          .fold[DockerReadyChecker](AlwaysReady) { _.toReadyChecker }

      DockerContainer(
        image = `image-name`,
        command = command,
        bindPorts = bindPorts,
        env = `environmental-variables`,
        readyChecker = readyChecker
      )
    }
  }
}
