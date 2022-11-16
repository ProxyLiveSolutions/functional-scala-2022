package com.onairentertainment.functional_scala_2022

import cats.~>
import cats.Show
import cats.MonadError
import cats.syntax.applicativeError.*
import cats.syntax.show.*

object ErrorLogger:
  def logErrorsK[F[_], E: Show](serviceName: String, log: String => F[Unit])(using ME: MonadError[F, E]): F ~> F =
    new (F ~> F):
      override def apply[A](fa: F[A]): F[A] = fa.onError { case error =>
        log(show"$serviceName has failed with an error[$error]")
      }
