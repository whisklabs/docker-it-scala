package whisk.docker

import java.net.{ HttpURLConnection, URL }

import com.github.dockerjava.api.DockerClient

import scala.concurrent.{ Promise, ExecutionContext, Future }

trait DockerReadyChecker extends (DockerContainer => Future[Boolean]) {
  def apply(container: DockerContainer): Future[Boolean]

  def and(other: DockerReadyChecker)(implicit ec: ExecutionContext) = {
    val s = this
    new DockerReadyChecker {
      override def apply(container: DockerContainer) = {
        val aF = s(container)
        val bF = other(container)
        for {
          a <- aF
          b <- bF
        } yield a && b
      }
    }
  }

  def or(other: DockerReadyChecker)(implicit ec: ExecutionContext) = {
    val s = this
    new DockerReadyChecker {
      override def apply(container: DockerContainer): Future[Boolean] = {
        val aF = s(container)
        val bF = other(container)
        val p = Promise[Boolean]()
        aF.map {
          case true => p.trySuccess(true)
          case _ =>
        }
        bF.map {
          case true => p.trySuccess(true)
          case _ =>
        }
        p.future
      }
    }
  }

  // TODO: implement looping for a ready check, for N retries, with D delta
  def loop() = this
}

object DockerReadyChecker {
  // TODO: get it from config?
  val dockerHost = "192.168.59.103"

  object Always extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = Future.successful(true)
  }

  case class HttpResponseCode(port: Int, path: String = "/", host: String = dockerHost, code: Int = 200)(implicit dc: DockerClient, ec: ExecutionContext) extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = {
      container.getPorts().map(_(port)).flatMap { p =>
        val url = new URL("http", host, p, path)
        Future {
          val con = url.openConnection().asInstanceOf[HttpURLConnection]
          con.getResponseCode == code
        }
      }
    }
  }

  case class F(f: DockerContainer => Future[Boolean]) extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = f(container)
  }
}
