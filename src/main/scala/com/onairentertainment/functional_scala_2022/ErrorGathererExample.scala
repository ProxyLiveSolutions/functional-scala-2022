package com.onairentertainment.functional_scala_2022

import cats.{Applicative, Functor, MonadError, MonadThrow}
import cats.syntax.traverse.*
import cats.mtl.Tell
import cats.data.{EitherT, WriterT}
import cats.effect.kernel.Temporal
import cats.effect.{GenTemporal, IO, IOApp}

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

object ErrorGathererExample extends IOApp.Simple:
  private final case class ExampleError(msg: String)
  private type MonadExampleErr[F[_]] = MonadError[F, ExampleError]
  private type TellExampleErr[F[_]]  = Tell[F, List[ExampleError]]
  private type LogEff[T]             = WriterT[IO, List[ExampleError], T]
  private type ErrEff[T]             = EitherT[LogEff, ExampleError, T]
  private type Eff[T]                = ErrEff[T]

//  /** Experiment of derivation for an instance of Tell for any Effect-monad and any collection as a log holder */
//  given instance[F[_]: Applicative, Col[_]: Applicative, A]: Tell[[X] =>> WriterT[F, Col[A], X], A] =
//    new Tell[[X] =>> WriterT[F, Col[A], X], A]:
//      def functor: Functor[[X] =>> WriterT[F, Col[A], X]] = Functor[[X] =>> WriterT[F, Col[A], X]]
//      def tell(l: A): WriterT[F, Col[A], Unit]            = WriterT.tell(Applicative[Col].pure(l))

  override def run: IO[Unit] = for
    (errs, either) <- logic[Eff].value.run
    _              <- IO.println(s"result: $either")
    _              <- IO.println(s"gathered errs: $errs")
    _              <- IO.println("The End!")
  yield ()

  private def logic[F[_]: MonadExampleErr: TellExampleErr: Temporal]: F[Unit] =
    val failedF = MonadError[F, ExampleError].raiseError[Unit](ExampleError("Hi there"))
    val gatherK = ErrorGatherer.gatherErrorsK[F, ExampleError]
    val retryK  = ErrorRetry.simpleRetryK[F, ExampleError]

    retryK(gatherK(failedF))
