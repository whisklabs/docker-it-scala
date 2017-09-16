package com.whisk.docker.testkit

import java.net.{HttpURLConnection, URL}
import java.sql.DriverManager
import java.util.{Timer, TimerTask}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}

class FailFastCheckException(m: String) extends Exception(m)

trait DockerReadyChecker {

  def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                      ec: ExecutionContext): Future[Unit]

  def and(other: DockerReadyChecker)(implicit docker: ContainerCommandExecutor,
                                     ec: ExecutionContext) = {
    val s = this
    DockerReadyChecker.F { container =>
      val aF = s(container)
      val bF = other(container)
      for {
        a <- aF
        b <- bF
      } yield {
        ()
      }
    }
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
        case e: FailFastCheckException => Future.failed(e)
        case e if rest > 0 =>
          withDelay(delay.toMillis)(attempt(rest - 1))
        case e =>
          Future.failed(e)
      }
    }

    attempt(attempts)
  }
}

object DockerReadyChecker {

  object Always extends DockerReadyChecker {
    override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                                 ec: ExecutionContext): Future[Unit] =
      Future.successful(())
  }

  case class HttpResponseCode(port: Int,
                              path: String = "/",
                              host: Option[String] = None,
                              code: Int = 200)
      extends DockerReadyChecker {

    override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                                 ec: ExecutionContext): Future[Unit] = {

      val p = container.mappedPorts()(port)
      val url = new URL("http", host.getOrElse(docker.client.getHost), p, path)
      Future {
        scala.concurrent.blocking {
          val con = url.openConnection().asInstanceOf[HttpURLConnection]
          try {
            if (con.getResponseCode != code)
              throw new Exception("unexpected response code: " + con.getResponseCode)
          } catch {
            case e: java.net.ConnectException =>
              throw e
          }
        }
      }
    }
  }

  case class LogLineContains(str: String) extends DockerReadyChecker {

    override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                                 ec: ExecutionContext): Future[Unit] = {
      container.state() match {
        case ContainerState.Ready(_) =>
          Future.successful(())
        case state: ContainerState.HasId =>
          docker
            .withLogStreamLinesRequirement(state.id, withErr = true)(_.contains(str))
            .map(_ => ())
        case _ =>
          Future.failed(
            new FailFastCheckException("can't initialise LogStream to container without Id"))
      }
    }
  }

  private[docker] case class TimeLimited(underlying: DockerReadyChecker, duration: FiniteDuration)
      extends DockerReadyChecker {

    override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                                 ec: ExecutionContext): Future[Unit] = {
      RetryUtils.runWithin(underlying(container), duration)
    }
  }

  private[docker] case class Looped(underlying: DockerReadyChecker,
                                    attempts: Int,
                                    delay: FiniteDuration)
      extends DockerReadyChecker {

    override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                                 ec: ExecutionContext): Future[Unit] = {

      def attempt(attemptsLeft: Int): Future[Unit] = {
        underlying(container)
          .recoverWith {
            case e: FailFastCheckException => Future.failed(e)
            case e if attemptsLeft > 0 =>
              RetryUtils.withDelay(delay.toMillis)(attempt(attemptsLeft - 1))
            case e =>
              Future.failed(e)
          }
      }

      attempt(attempts)
    }
  }

  private[docker] case class F(f: BaseContainer => Future[Unit]) extends DockerReadyChecker {
    override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                                 ec: ExecutionContext): Future[Unit] =
      f(container)
  }

  case class Jdbc(driverClass: String,
                  user: String,
                  password: String,
                  database: Option[String] = None,
                  port: Option[Int] = None)
      extends DockerReadyChecker {

    private val driverLower = driverClass.toLowerCase
    private[Jdbc] val dbms: String = if (driverLower.contains("mysql")) {
      "mysql"
    } else if (driverLower.contains("postgres")) {
      "postgresql"
    } else {
      throw new IllegalArgumentException("unsupported database for ready check")
    }

    override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                                 ec: ExecutionContext): Future[Unit] = {

      Future(scala.concurrent.blocking {
        try {
          Class.forName(driverClass)
          val p = port match {
            case Some(v) => container.mappedPort(v)
            case None    => container.mappedPorts().head._2
          }

          val url = "jdbc:" + dbms + "://" + docker.client.getHost + ":" + p + "/" + database
            .getOrElse("")

          val connection = Option(DriverManager.getConnection(url, user, password))
          connection.foreach(_.close())
          if (connection.isEmpty) {
            throw new Exception(s"can't establish jdbc connection to $url")
          }
        } catch {
          case e: ClassNotFoundException =>
            throw new FailFastCheckException(s"jdbc class $driverClass not found")
        }
      })
    }
  }

}
