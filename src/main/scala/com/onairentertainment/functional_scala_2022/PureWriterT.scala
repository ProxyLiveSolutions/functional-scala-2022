package com.onairentertainment.functional_scala_2022
import cats.FlatMap
import cats.{Applicative, Functor, Semigroup}
import cats.syntax.semigroup.*

final case class PureWriterT[F[_], L, V](run: (L, F[V])):
  def tell(l: L)(using Semigroup[L]): PureWriterT[F, L, V] =
    PureWriterT((run._1.combine(l), run._2))

  def map[B](f: V => B)(using Functor[F]): PureWriterT[F, L, B] =
    import cats.syntax.functor.*
    PureWriterT(run._1, run._2.map(f))

  def flatMap[B](f: V => PureWriterT[F, L, B])(using F: FlatMap[F], S: Semigroup[L]): PureWriterT[F, L, B] =
    ???

object PureWriterT:
  import cats.mtl.Tell
  given pureWriterTFunctor[F[_]: Functor, L]: Functor[[X] =>> PureWriterT[F, L, X]] =
    new Functor[[X] =>> PureWriterT[F, L, X]]:
      override def map[A, B](fa: PureWriterT[F, L, A])(f: A => B): PureWriterT[F, L, B] = fa.map(f)

  given pureWriterTTell[F[_]: Applicative, L]: Tell[[X] =>> PureWriterT[F, L, X], L] =
    new Tell[[X] =>> PureWriterT[F, L, X], L]:
      override def functor: Functor[[X] =>> PureWriterT[F, L, X]] = Functor[[X] =>> PureWriterT[F, L, X]]
      override def tell(l: L): PureWriterT[F, L, Unit]            = PureWriterT((l, Applicative[F].unit))
