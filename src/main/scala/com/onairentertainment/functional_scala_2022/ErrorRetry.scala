package com.onairentertainment.functional_scala_2022

import cats.{MonadError, ~>}
import cats.syntax.flatMap.*
import cats.effect.{GenTemporal, Temporal}
import com.onairentertainment.functional_scala_2022.tycl.Sleep

import scala.concurrent.duration.*

object ErrorRetry:
  private val SleepTime = 10.milli
  private val Attempts  = 5
  def simpleRetryK[F[_], E](using ME: MonadError[F, E], S: Sleep[F]): F ~> F = new (F ~> F):
    override def apply[A](fa: F[A]): F[A] =
      def internal(remain: Int): F[A] =
        ME.handleErrorWith(fa) { err =>
          if (remain <= 0)
            ME.raiseError(err)
          else
            S.sleep(SleepTime) >> internal(remain - 1)
        }
      internal(Attempts)
