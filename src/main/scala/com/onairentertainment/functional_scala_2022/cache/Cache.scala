package com.onairentertainment.functional_scala_2022.cache

import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.effect.Ref
import cats.MonadError
import Cache.{InsertResult, UpdateResult}
import cats.Functor
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError

trait Cache[F[_], K, V]:
  def get(key: K): F[Option[V]]
  def insert(key: K, value: V): F[InsertResult]
  def update(key: K, value: V): F[UpdateResult]

object Cache:
  type LowLvlErrorGen[F[_]] = F[Option[LowLevelError]]
  enum UpdateResult:
    case Success
    case KeyNotFound

  enum InsertResult:
    case Success
    case KeyAlreadyExists

  def refBased[F[_]: Functor, K, V](ref: Ref[F, Map[K, V]]): Cache[F, K, V] = RefBasedCacheImpl(ref)

  def errorProne[F[_]: MonadBLError, K, V](
                                            underlying: Cache[F, K, V],
                                            errorGen: LowLvlErrorGen[F]
  ): Cache[F, K, V] =
    // Disclaimer:
    // Here we have to use `MonadBLError[..]` as in the current implementation we stick with the type classes provided
    // by cats. And cats' MonadError[..] is invariant over an error type as it provides both abilities at the same time
    // - to raise an error and to handle it. This can be overcome by using `Raise[..]` from tofu or cats-mtl.
    ErrorProneCacheImpl(underlying, errorGen)
