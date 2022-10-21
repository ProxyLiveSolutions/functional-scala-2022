package com.onairentertainment.functional_scala_2022.account

import cats.Applicative
import com.onairentertainment.functional_scala_2022.cache.SimpleCache
import BusinessLevelError.{MonadBLError, TxAlreadyExists}
import cats.syntax.flatMap.*
import com.onairentertainment.functional_scala_2022.cache.SimpleCache.InsertResult

trait TxDb[F[_]]:
  def saveTx(tx: Tx): F[Unit]

object TxDb:
  type TxCache[F[_]] = SimpleCache[F, TxId, TxInfo]
  private final class CacheBasedImpl[F[_]](cache: TxCache[F])(using M: MonadBLError[F]) extends TxDb[F]:
    override def saveTx(tx: Tx): F[Unit] = cache.insert(tx.id, tx.info).flatMap {
      case InsertResult.Success          => M.unit
      case InsertResult.KeyAlreadyExists => M.raiseError(TxAlreadyExists(tx.id))
    }

  def make[F[_]: MonadBLError](cache: TxCache[F]): TxDb[F] = new CacheBasedImpl[F](cache)
