package com.onairentertainment.functional_scala_2022.tycl

import scala.concurrent.duration.FiniteDuration
import cats.effect.Temporal

// `Timer[F[_]]` was removed from cats-effect without a proper replacement - GenTemporal is way too powerful to be useful.
// But let's add it here as we need it for retries
trait Sleep[F[_]]:
  def sleep(duration: FiniteDuration): F[Unit]

object Sleep:
  def apply[F[_]](using S: Sleep[F]): Sleep[F] = S

  given sleepFromTemporal[F[_]: Temporal]: Sleep[F] =
    (duration: FiniteDuration) => Temporal[F].sleep(duration)
