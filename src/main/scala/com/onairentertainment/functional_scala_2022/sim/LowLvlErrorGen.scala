package com.onairentertainment.functional_scala_2022.sim

import cats.effect.Sync
import com.onairentertainment.functional_scala_2022.cache.LowLevelError
import cats.effect.std.Random
import cats.Functor
import cats.syntax.functor.*

type LowLvlErrorGen[F[_]] = F[Option[LowLevelError]]
object LowLvlErrorGen {
  def random[F[_]: Functor: Random](errorRate: Double, critErrorRate: Double): LowLvlErrorGen[F] =
    for rnd <- Random[F].betweenDouble(0.0d, 1.0d)
    yield
      if (rnd <= errorRate)
        if (rnd <= critErrorRate)
          Some(LowLevelError.DbCriticalError)
        else
          Some(LowLevelError.DbError)
      else
        None
}
