package whisk.docker

import java.net.{ HttpURLConnection, URL }
import java.util.{ Timer, TimerTask }

import com.github.dockerjava.api.DockerClient

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future, Promise }

trait DockerReadyChecker extends (DockerContainer => Future[Boolean]) {
  def apply(container: DockerContainer): Future[Boolean]

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

  def looped(attempts: Int, delay: Duration)(implicit ec: ExecutionContext): DockerReadyChecker = {
    val checker = this.apply _
    DockerReadyChecker.F { container =>
      val timer = new Timer

      def makeTask[T](future: => Future[T])(schedule: TimerTask => Unit): Future[T] = {
        val prom = Promise[T]()
        schedule(
          new TimerTask {
            def run() {
              try {
                prom.completeWith(future)
              } catch {
                case ex: Throwable => prom.failure(ex)
              }
            }
          }
        )
        prom.future
      }

      def schedule[T](delay: Long)(body: => Future[T])(implicit ctx: ExecutionContext): Future[T] = {
        makeTask(body)(timer.schedule(_, delay))
      }

      def connectionLoop[T](f: => Future[T], attempts: Int, delay: Long): Future[T] = {
        val future = schedule(delay)(f)
        if (attempts <= 1) future else future.recoverWith({ case _ => connectionLoop(f, attempts - 1, delay) })
      }

      connectionLoop(checker(container), attempts, delay.toMillis)
    }
  }
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
