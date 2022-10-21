package com.onairentertainment.functional_scala_2022.cache

import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.effect.Ref
import cats.MonadError
import SimpleCache.{InsertResult, UpdateResult}
import cats.Functor
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError

trait SimpleCache[F[_], K, V]:
  def get(key: K): F[Option[V]]
  def insert(key: K, value: V): F[InsertResult]
  def update(key: K, value: V): F[UpdateResult]

object SimpleCache:
  type LowLvlErrorGen[F[_]] = F[Option[LowLevelError]]
  enum UpdateResult:
    case Success
    case KeyNotFound

  enum InsertResult:
    case Success
    case KeyAlreadyExists

  def refBased[F[_]: Functor, K, V](ref: Ref[F, Map[K, V]]): SimpleCache[F, K, V] = RefBasedCacheImpl(ref)
  def errorProne[F[_], K, V](
      underlying: SimpleCache[F, K, V],
      errorGen: LowLvlErrorGen[F]
  )(using MonadError[F, LowLevelError]): SimpleCache[F, K, V] = ErrorProneCacheImpl(underlying, errorGen)
