package com.whisk.docker

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{Future, Promise}

/**
  * General utility functions
  */
package object testkit {
  implicit class OptionalOps[A](val content: A) extends AnyVal {
    def withOption[B](optional: Option[B])(f: (A, B) => A): A = optional match {
      case None    => content
      case Some(x) => f(content, x)
    }
  }

  private[docker] class SinglePromise[T] {
    val promise: Promise[T] = Promise[T]()

    def future: Future[T] = promise.future

    val flag = new AtomicBoolean(false)

    def init(f: => Future[T]): Future[T] = {
      if (!flag.getAndSet(true)) {
        promise.tryCompleteWith(f)
      }
      future
    }
  }

  private[docker] object SinglePromise {
    def apply[T] = new SinglePromise[T]
  }

}
