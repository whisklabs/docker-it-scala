package com.whisk.docker

import java.net.{ HttpURLConnection, URL }
import java.util.{TimerTask, Timer}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ TimeoutException, ExecutionContext, Future, Promise }
import scala.util.Try

trait DockerReadyChecker {

  def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext): Future[Boolean]

  def and(other: DockerReadyChecker)(implicit docker: Docker, ec: ExecutionContext) = {
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

  def or(other: DockerReadyChecker)(implicit docker: Docker, ec: ExecutionContext) = {
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

  def within(duration: FiniteDuration)(implicit docker: Docker, ec: ExecutionContext): DockerReadyChecker = {
    DockerReadyChecker.TimeLimited(this, duration)
  }

  def looped(attempts: Int, delay: FiniteDuration)(implicit docker: Docker, ec: ExecutionContext): DockerReadyChecker = {
    DockerReadyChecker.Looped(this, attempts, delay)
  }
}

object DockerReadyChecker {

  private def withDelay[T](delay: Long)(f: => Future[T]): Future[T] = {
    val timer = new Timer()
    val promise = Promise[T]()
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        promise.completeWith(f)
        timer.cancel()
      }
    }, delay)
    promise.future
  }

  private def runWithin[T](future: => Future[T], deadline: FiniteDuration)(implicit ec: ExecutionContext): Future[T] = {
    val bail = Promise[T]()
    withDelay(deadline.toMillis)(bail.tryCompleteWith(Future.failed(new TimeoutException(s"timed out after $deadline"))).future)
    Future.firstCompletedOf(future :: bail.future :: Nil)
  }

  object Always extends DockerReadyChecker {
    override def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] =
      Future.successful(true)
  }

  case class HttpResponseCode(port: Int, path: String = "/", host: Option[String] = None, code: Int = 200) extends DockerReadyChecker {
    override def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] = {
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

  case class LogLineContains(str: String) extends DockerReadyChecker {
    override def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext) = {
      container.withLogStreamLines(withErr = true) { case frame if frame.toString.contains(str) => true }
    }
  }

  private[docker] case class TimeLimited(underlying: DockerReadyChecker, duration: FiniteDuration) extends DockerReadyChecker {

    override def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] = {
      runWithin(underlying(container), duration).recover {
        case _: TimeoutException =>
          false
      }
    }
  }

  private[docker] case class Looped(underlying: DockerReadyChecker, attempts: Int, delay: FiniteDuration) extends DockerReadyChecker {

    override def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] = {
      def attempt(rest: Int): Future[Boolean] = {
        underlying(container).filter(identity).recoverWith {
          case e =>
            rest match {
              case 0 =>
                Future.failed(e match {
                  case _: NoSuchElementException =>
                    new NoSuchElementException(s"Ready checker returned false after $attempts attempts, delayed $delay each")
                  case _ => e
                })
              case n =>
                withDelay(delay.toMillis)(attempt(n - 1))
            }
        }
      }

      attempt(attempts)
    }
  }

  case class F(f: DockerContainer => Future[Boolean]) extends DockerReadyChecker {
    override def apply(container: DockerContainer)(implicit docker: Docker, ec: ExecutionContext): Future[Boolean] =
      f(container)
  }
}
