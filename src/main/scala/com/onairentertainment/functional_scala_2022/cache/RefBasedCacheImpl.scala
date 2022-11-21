package com.onairentertainment.functional_scala_2022.cache

import cats.syntax.functor.*
import cats.Functor
import cats.effect.Ref
import com.onairentertainment.functional_scala_2022.cache.Cache.{InsertResult, UpdateResult}

private[cache] final class RefBasedCacheImpl[F[_]: Functor, K, V](ref: Ref[F, Map[K, V]]) extends Cache[F, K, V]:
  override def get(key: K): F[Option[V]] = ref.get.map(_.get(key))

  override def insert(key: K, value: V): F[InsertResult] = ref.modify { oldMap =>
    if (oldMap.contains(key))
      (oldMap, InsertResult.KeyAlreadyExists)
    else
      (oldMap.updated(key, value), InsertResult.Success)
  }

  override def update(key: K, value: V): F[UpdateResult] = ref.modify { oldMap =>
    if (!oldMap.contains(key))
      (oldMap, UpdateResult.KeyNotFound)
    else
      (oldMap.updated(key, value), UpdateResult.Success)
  }
