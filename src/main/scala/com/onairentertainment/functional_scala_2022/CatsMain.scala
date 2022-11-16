package com.onairentertainment.functional_scala_2022

import cats.syntax.functor.*
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import cats.{Applicative, Defer}
import cats.data.EitherT
import cats.effect.{IO, IOApp, Ref, Sync}
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError

import java.util.UUID
import scala.util.control.NoStackTrace
import cats.effect.std.Random

object CatsMain extends IOApp.Simple:
  override def run: IO[Unit] =
    type AppEffect[A] = EitherT[IO, BusinessLevelError, A]

    val execution = for
      given Random[AppEffect] <- Random.scalaUtilRandom[AppEffect]
      _                       <- App.runApp[AppEffect]()
    yield ()

    execution.leftMap(AppException.apply).value.flatMap(IO.fromEither)
