package whisk.docker

import java.net.{ HttpURLConnection, URL }

import com.spotify.docker.client.DockerClient.LogsParameter
import com.spotify.docker.client.LogStream

import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ TimeoutException, ExecutionContext, Future, Promise }

trait DockerReadyChecker extends (DockerContainer => Future[Boolean]) {

  def and(other: DockerReadyChecker)(implicit ec: ExecutionContext) = {
    val s = this
    DockerReadyChecker.F { container =>
      val aF = s(container)
      val bF = other(container)
      for {
        a <- aF
        b <- bF
      } yield a && b
    }
  }

  def or(other: DockerReadyChecker)(implicit ec: ExecutionContext) = {
    val s = this
    DockerReadyChecker.F { container =>
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

  def within(duration: FiniteDuration)(implicit ec: ExecutionContext): DockerReadyChecker = {
    val checker = this.apply _
    DockerReadyChecker.F { container =>
      import undelay._

      checker(container).within(duration).recover {
        case _: TimeoutException =>
          false
      }
    }
  }

  def looped(attempts: Int, delay: FiniteDuration)(implicit ec: ExecutionContext): DockerReadyChecker = {
    val checker = this.apply _
    DockerReadyChecker.F { container =>

      def attempt(rest: Int): Future[Boolean] = {
        checker(container).filter(identity).recoverWith {
          case e =>
            rest match {
              case 0 =>
                Future.failed(e)
              case n =>
                odelay.Delay(delay)(attempt(n - 1)).future.flatMap(identity)
            }
        }
      }

      attempt(attempts)
    }
  }
}

object DockerReadyChecker {

  object Always extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = Future.successful(true)
  }

  case class HttpResponseCode(port: Int, path: String = "/", host: Option[String] = None, code: Int = 200)(implicit docker: Docker, ec: ExecutionContext) extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = {
      container.getPorts().map(_(port)).flatMap { p =>
        val url = new URL("http", host.getOrElse(docker.host), p, path)
        Future {
          val con = url.openConnection().asInstanceOf[HttpURLConnection]
          try {
            con.getResponseCode == code
          } catch {
            case e: java.net.ConnectException =>
              false
          }
        }
      }
    }
  }

  case class LogLine(check: String => Boolean)(implicit docker: Docker, ec: ExecutionContext) extends DockerReadyChecker {
    override def apply(container: DockerContainer) = {
      @tailrec
      def pullAndCheck(it: LogStream): Boolean = it.hasNext match {
        case true =>
          val s = it.next()
          if (check(io.Source.fromBytes(s.content().array())(io.Codec.ISO8859).mkString)) true
          else pullAndCheck(it)
        case false =>
          false
      }

      for {
        id <- container.id
        is <- Future(docker.client.logs(id, LogsParameter.FOLLOW, LogsParameter.STDOUT))
      } yield pullAndCheck(is)

    }
  }

  case class F(f: DockerContainer => Future[Boolean]) extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = f(container)
  }
}
