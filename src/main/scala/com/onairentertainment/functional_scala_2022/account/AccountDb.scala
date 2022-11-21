package com.onairentertainment.functional_scala_2022.account

import cats.syntax.flatMap.*
import com.onairentertainment.functional_scala_2022.cache.Cache
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.{AccountNotFound, MonadBLError}
import com.onairentertainment.functional_scala_2022.cache.Cache.UpdateResult

trait AccountDb[F[_]]:
  def findAccount(id: UserId): F[Account]
  def updateAccount(account: Account): F[Unit]

object AccountDb:
  type AccountCache[F[_]] = Cache[F, UserId, Money]

  private final class CacheBasedImpl[F[_]](cache: AccountCache[F])(using M: MonadBLError[F]) extends AccountDb[F]:
    override def findAccount(id: UserId): F[Account] = cache.get(id).flatMap {
      case Some(value) => M.pure(Account(id, value))
      case None        => M.raiseError(AccountNotFound(id))
    }
    override def updateAccount(account: Account): F[Unit] = cache.update(account.owner, account.balance).flatMap {
      case UpdateResult.Success     => M.unit
      case UpdateResult.KeyNotFound => M.raiseError(AccountNotFound(account.owner))
    }

  def make[F[_]: MonadBLError](cache: AccountCache[F]): AccountDb[F] = new CacheBasedImpl[F](cache)
