package com.onairentertainment.functional_scala_2022.cache

import cats.MonadError
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError
import com.onairentertainment.functional_scala_2022.cache.SimpleCache.{InsertResult, LowLvlErrorGen, UpdateResult}

private final class ErrorProneCacheImpl[F[_], K, V](
    underlying: SimpleCache[F, K, V],
    errorGen: LowLvlErrorGen[F]
)(using
    M: MonadBLError[F]
) extends SimpleCache[F, K, V]:
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
