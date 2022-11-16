package com.onairentertainment.functional_scala_2022

import cats.{MonadError, ~>}
import cats.syntax.flatMap.*
import cats.effect.{GenTemporal, Temporal}
import cats.mtl.Handle

import scala.concurrent.duration.*

object ErrorRetry:
  def simpleRetryK[F[_], E](using H: Handle[F, E], T: Temporal[F]): F ~> F = new (F ~> F):
    override def apply[A](fa: F[A]): F[A] =
      def internal(remain: Int): F[A] =
        H.handleWith(fa) { err =>
          if (remain <= 0)
            H.raise(err)
          else
            T.sleep(10.milli) >> internal(remain - 1)
        }
      internal(5)
