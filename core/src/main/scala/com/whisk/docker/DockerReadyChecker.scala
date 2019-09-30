package com.whisk.docker

import java.net.{HttpURLConnection, URL}
import java.util.{Timer, TimerTask}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}

trait DockerReadyChecker {

  def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                             ec: ExecutionContext, timeout: Duration): Future[Boolean]

  @deprecated("this method will be removed. Use DockerReadyChecker.And(a, b)", "0.9.6")
  def and(other: DockerReadyChecker): DockerReadyChecker = {
    DockerReadyChecker.And(this, other)
  }

  @deprecated("this method will be removed. Use DockerReadyChecker.Or(a, b)", "0.9.6")
  def or(other: DockerReadyChecker): DockerReadyChecker = {
    DockerReadyChecker.Or(this, other)
  }

  def within(duration: FiniteDuration): DockerReadyChecker = {
    DockerReadyChecker.TimeLimited(this, duration)
  }

  def looped(attempts: Int, delay: FiniteDuration): DockerReadyChecker = {
    DockerReadyChecker.Looped(this, attempts, delay)
  }
}

object RetryUtils {

  def withDelay[T](delay: Long)(f: => Future[T]): Future[T] = {
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

  def runWithin[T](future: => Future[T], deadline: FiniteDuration)(
      implicit ec: ExecutionContext): Future[T] = {
    val bail = Promise[T]()
    withDelay(deadline.toMillis)(
      bail
        .tryCompleteWith(Future.failed(new TimeoutException(s"timed out after $deadline")))
        .future)
    Future.firstCompletedOf(future :: bail.future :: Nil)
  }

  def looped[T](future: => Future[T], attempts: Int, delay: FiniteDuration)(
      implicit ec: ExecutionContext): Future[T] = {
    def attempt(rest: Int): Future[T] = {
      future.recoverWith {
        case e =>
          rest match {
            case 0 =>
              Future.failed(e match {
                case _: NoSuchElementException =>
                  new NoSuchElementException(
                    s"Ready checker returned false after $attempts attempts, delayed $delay each")
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

object DockerReadyChecker {

  case class And(r1: DockerReadyChecker, r2: DockerReadyChecker) extends DockerReadyChecker {
    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] = {
      val aF = r1(container)
      val bF = r2(container)
      for {
        a <- aF
        b <- bF
      } yield a && b
    }
  }

  case class Or(r1: DockerReadyChecker, r2: DockerReadyChecker) extends DockerReadyChecker {
    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] = {
      val aF = r1(container)
      val bF = r2(container)
      val p = Promise[Boolean]()
      aF.map {
        case true => p.trySuccess(true)
        case _    =>
      }
      bF.map {
        case true => p.trySuccess(true)
        case _    =>
      }
      p.future
    }
  }

  object Always extends DockerReadyChecker {
    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] =
      Future.successful(true)
  }

  case class HttpResponseCode(port: Int,
                              path: String = "/",
                              host: Option[String] = None,
                              code: Int = 200)
      extends DockerReadyChecker {
    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] = {
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
    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] = {
      for {
        id <- container.id
        _ <- docker.withLogStreamLinesRequirement(id, withErr = true)(_.contains(str))
      } yield {
        true
      }
    }
  }

  private[docker] case class TimeLimited(underlying: DockerReadyChecker, duration: FiniteDuration)
      extends DockerReadyChecker {

    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] = {
      RetryUtils.runWithin(underlying(container), duration).recover {
        case _: TimeoutException =>
          false
      }
    }
  }

  private[docker] case class Looped(underlying: DockerReadyChecker,
                                    attempts: Int,
                                    delay: FiniteDuration)
      extends DockerReadyChecker {

    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] = {
      RetryUtils.looped(underlying(container).filter(identity), attempts, delay)
    }
  }

  case class F(f: DockerContainerState => Future[Boolean]) extends DockerReadyChecker {
    override def apply(container: DockerContainerState)(implicit docker: DockerCommandExecutor,
                                                        ec: ExecutionContext, timeout: Duration): Future[Boolean] =
      f(container)
  }

}
