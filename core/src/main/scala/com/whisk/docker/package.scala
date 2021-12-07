package com.whisk

import java.util.TimerTask
import java.util.concurrent.{Callable, CancellationException, FutureTask}

import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Try}

/**
  * General utility functions
  */
package object docker {
  implicit class OptionalOps[A](val content: A) extends AnyVal {
    def withOption[B](optional: Option[B])(f: (A, B) => A): A = optional match {
      case None    => content
      case Some(x) => f(content, x)
    }
  }

  private[docker] object PerishableFuture {

    def apply[T](body: => T)(implicit ec: ExecutionContext, timeout: Duration): Future[T] = timeout match {
      case finiteTimeout: FiniteDuration =>
        val promise = Promise[T]

        val futureTask = new FutureTask[T](new Callable[T] {
          override def call(): T = body
        }) {
          override def done(): Unit = promise.tryComplete {
            Try(get()).recoverWith {
              case _: CancellationException => Failure(new TimeoutException())
            }
          }
        }

        val reaperTask = new TimerTask {
          override def run(): Unit = {
            futureTask.cancel(true)
            promise.tryFailure(new TimeoutException())
          }
        }

        timer.schedule(reaperTask, finiteTimeout.toMillis)
        ec.execute(futureTask)

        promise.future

      case _ => Future.apply(body)
    }

    private val timer = new java.util.Timer(true)

  }
}
