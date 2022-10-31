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

object CatsMain extends IOApp.Simple:
  override def run: IO[Unit] =
    type AppEffect[A] = EitherT[IO, BusinessLevelError, A]
    App.runApp[AppEffect]().leftMap(AppException.apply).value.flatMap(IO.fromEither)
