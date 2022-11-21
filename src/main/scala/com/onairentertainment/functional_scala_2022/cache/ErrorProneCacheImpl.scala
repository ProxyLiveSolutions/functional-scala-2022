package com.onairentertainment.functional_scala_2022.cache

import cats.MonadError
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError
import com.onairentertainment.functional_scala_2022.cache.Cache.{InsertResult, LowLvlErrorGen, UpdateResult}

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap

private final class ErrorProneCacheImpl[F[_], K, V](
    underlying: Cache[F, K, V],
    errorGen: LowLvlErrorGen[F]
)(using
    M: MonadBLError[F]
) extends Cache[F, K, V]:
  // This function can be defined as a natural transformation itself
  private def wrapWithError[A](fa: F[A]): F[A] = for
    maybeError <- errorGen
    _ <- maybeError match
      case Some(error) => M.raiseError(BusinessLevelError.LowLevel(error))
      case None        => M.unit
    a <- fa
  yield a

  override def get(key: K): F[Option[V]]                 = wrapWithError(underlying.get(key))
  override def insert(key: K, value: V): F[InsertResult] = wrapWithError(underlying.insert(key, value))
  override def update(key: K, value: V): F[UpdateResult] = wrapWithError(underlying.update(key, value))

object Main extends App:
  final case class Accum(sieve: Sieve, foundNumbers: List[Int])

  type Sieve = SortedMap[Int, Boolean]
  private val N = 100
  @tailrec
  private def dropFromSieve(elem: Int, offset: Int, sieve: Sieve): Sieve =
    if (elem <= N)
      dropFromSieve(elem + offset, offset, sieve.removed(elem))
    else
      sieve

  val fullSieve = SortedMap.empty[Int, Boolean] ++ (2 to N).map(index => (index, true)).toMap
  val numbers   = fullSieve.keys.toList
  val zero      = Accum(fullSieve, List.empty)
  val result = numbers.foldLeft(zero) { (acc, elem) =>
    val foundNumbers =
      if (acc.sieve.contains(elem))
        acc.foundNumbers.prepended(elem)
      else
        acc.foundNumbers

    val nextSieve = dropFromSieve(elem, elem, acc.sieve)
    Accum(nextSieve, foundNumbers)
  }

  println(result.foundNumbers.reverse)
