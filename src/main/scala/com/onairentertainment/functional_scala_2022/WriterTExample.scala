package com.onairentertainment.functional_scala_2022

import cats.data.WriterT
import cats.effect.{ExitCode, IO, IOApp}
import cats.instances.list.*
import cats.mtl.{Listen, Tell}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import cats.{Applicative, Functor, Monad}

/** Example of using the "WriterT" monad transformer to collect log messages inside an effect.
  *
  * That makes possible to have log messages as a result(List of strings) of a computation not as a delayed computation.
  * This approach can be used for any `A => F[Unit]` like computations by gathering instances of `A` into an result
  * effect.
  */
object WriterTExample extends IOApp:
  type ListLog  = List[String]
  type ChainLog = List[String]
  type Log      = ListLog
  type Eff[T]   = WriterT[IO, Log, T]

  /** Experiment of derivation for an instance of Tell for any Effect-monad and any collection as a log holder */
  given instance[F[_]: Applicative, Col[_]: Applicative, A]: Tell[[X] =>> WriterT[F, Col[A], X], A] =
    new Tell[[X] =>> WriterT[F, Col[A], X], A] {
      def functor: Functor[[X] =>> WriterT[F, Col[A], X]] = Functor[[X] =>> WriterT[F, Col[A], X]]

      def tell(l: A): WriterT[F, Col[A], Unit] = WriterT.tell(Applicative[Col].pure(l))
    }

  override def run(args: List[String]): IO[ExitCode] =
    val result: Eff[Unit] = logIntoEffect[Eff]()
    extractLogsFromEff(result, IO.println).as(ExitCode.Success)

  // Put logs into F[_] using the "Tell[..]" tycl
  private def logIntoEffect[F[_]: Monad]()(using T: Tell[F, String]): F[Unit] =
    for
      _ <- T.tell("first")
      _ <- T.tell("second")
      _ <- T.tell("third")
      _ <- T.tell("and so on")
    yield ()

  // Extract logs out of the box and write them into a console
  private def extractLogsFromEff[A](f: Eff[A], log: String => IO[Unit]): IO[A] =
    f.run.flatMap { case (logs, a) =>
      logs.traverse(log).as(a)
    }
