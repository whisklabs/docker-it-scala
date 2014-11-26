package whisk.docker

import java.net.{ HttpURLConnection, URL }
import java.util.{ Timer, TimerTask }

import com.github.dockerjava.api.DockerClient

import scala.concurrent.duration.{ FiniteDuration, Duration }
import scala.concurrent.{ ExecutionContext, Future, Promise }

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

      checker(container).within(duration)
    }
  }

  def looped(attempts: Int, delay: FiniteDuration)(implicit ec: ExecutionContext): DockerReadyChecker = {
    val checker = this.apply _
    DockerReadyChecker.F { container =>

      val p = Promise[Boolean]()

      def attempt(rest: Int): Unit = rest match {
        case 0 =>
          p tryCompleteWith checker(container)
        case n =>
          p tryCompleteWith checker(container)
          odelay.Delay(delay)(attempt(rest - 1))
      }
      attempt(attempts)

      p.future
    }
  }
}

object DockerReadyChecker {
  // TODO: get it from config? wrap DockerClient and its Config with a case class?
  val dockerHost = "192.168.59.103"

  object Always extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = Future.successful(true)
  }

  case class HttpResponseCode(port: Int, path: String = "/", host: String = dockerHost, code: Int = 200)(implicit dc: DockerClient, ec: ExecutionContext) extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = {
      container.getPorts().map(_(port)).flatMap { p =>
        val url = new URL("http", host, p, path)
        Future {
          println("trying to connect to " + url)
          val con = url.openConnection().asInstanceOf[HttpURLConnection]
          println("con = " + con)
          println("respcode = " + con.getResponseCode)
          con.getResponseCode == code
        }
      }
    }
  }

  case class F(f: DockerContainer => Future[Boolean]) extends DockerReadyChecker {
    override def apply(container: DockerContainer): Future[Boolean] = f(container)
  }

  // TODO: logs reader checker
}
