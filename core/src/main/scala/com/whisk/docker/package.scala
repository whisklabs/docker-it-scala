package com.whisk

/**
  * General utility functions
  */
package object docker {
  implicit class OptionalOps[A](val content: A) extends AnyVal {
    def withOption[B](optional: Option[B])(f: (A, B) => A): A = optional match {
      case None => content
      case Some(x) => f(content, x)
    }
  }
}
