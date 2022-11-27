package com.onairentertainment.functional_scala_2022

import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.{Applicative, Defer, Functor}
import cats.effect.Ref
import com.onairentertainment.functional_scala_2022.account.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError
import com.onairentertainment.functional_scala_2022.cache.Cache
import com.onairentertainment.functional_scala_2022.tycl.{FunctorK, Sleep}
import FunctorK.*

import java.util.UUID

object App:
  type AccountCache[F[_]] = Cache[F, UserId, Money]
  type TxCache[F[_]]      = Cache[F, TxId, TxInfo]
  private def delay[F[_]: Applicative: Defer, A](a: => A): F[A] = Defer[F].defer(Applicative[F].pure(a))
  private def log[F[_]: Applicative: Defer](str: String)        = delay(println(str))

  import cats.effect.std.Random
  import com.onairentertainment.functional_scala_2022.sim.LowLvlErrorGen
  private def makeCaches[F[_]: Functor](
      accRef: Ref[F, AccountMap],
      txRef: Ref[F, TxMap]
  ): (AccountCache[F], TxCache[F]) =
    val accountCache = Cache.refBased(accRef)
    val txCache      = Cache.refBased(txRef)
    (accountCache, txCache)

  private def makeFaultyCaches[F[_]: MonadBLError: Random](
      accRef: Ref[F, AccountMap],
      txRef: Ref[F, TxMap]
  ): (AccountCache[F], TxCache[F]) =
    val accountCache       = Cache.refBased(accRef)
    val failGen            = LowLvlErrorGen.random[F](0.2, 0.05)
    val faultyAccountCache = Cache.errorProne(accountCache, failGen)
    val txCache            = Cache.refBased(txRef)
    val faultyTxCache      = Cache.errorProne(txCache, failGen)
    (faultyAccountCache, faultyTxCache)

  private def makeService[F[_]: MonadBLError: Defer: Random](
      accountCache: AccountCache[F],
      txCache: TxCache[F]
  ): AccountService[F] =
    val txIdGen   = delay(UUID.randomUUID())
    val accountDb = AccountDb.make(accountCache)
    val txDb      = TxDb.make(txCache)
    AccountService.make(txIdGen, accountDb, txDb)

  def runApp[F[_]: Ref.Make: Random: Defer: MonadBLError: Sleep](): F[Unit] =
    import Constants.*
    val logErrorsK = ErrorLogger.logErrorsK[F, BusinessLevelError]("AccountService", log)
    val retryK     = ErrorRetry.simpleRetryK[F, BusinessLevelError]
    for
      accRef <- Ref.of[F, AccountMap](accounts)
      txRef  <- Ref.of[F, TxMap](txs)
      (accCache, txCache) = makeFaultyCaches(accRef, txRef)
      service = makeService(accCache, txCache) // Make an instance of the service
        .mapK(logErrorsK) // Wraps the service with logging
        .mapK(retryK)     // Wraps the service with retries
      beforeUser1 <- service.getAccount(user1)
      beforeUser2 <- service.getAccount(user2)
      _           <- log(s"Before: $beforeUser1, $beforeUser2")
      _           <- service.makeTransfer(TxInfo(user1, user2, 10))
      afterUser1  <- service.getAccount(user1)
      afterUser2  <- service.getAccount(user2)
      _           <- log(s"After: $afterUser1, $afterUser2")
    yield ()
