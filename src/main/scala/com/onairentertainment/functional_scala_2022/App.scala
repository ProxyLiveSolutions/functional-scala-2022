package com.onairentertainment.functional_scala_2022

import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.{Applicative, Defer}
import cats.effect.Ref
import com.onairentertainment.functional_scala_2022.account.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError
import com.onairentertainment.functional_scala_2022.cache.SimpleCache

import java.util.UUID

object App:
  private def delay[F[_]: Applicative: Defer, A](a: => A): F[A] = Defer[F].defer(Applicative[F].pure(a))
  private def log[F[_]: Applicative: Defer](str: String)        = delay(println(str))

  import cats.effect.std.Random
  import com.onairentertainment.functional_scala_2022.sim.LowLvlErrorGen
  private def makeFaultyService[F[_]: MonadBLError: Defer: Random](
      accRef: Ref[F, AccountMap],
      txRef: Ref[F, TxMap]
  ): AccountService[F] =
    val txIdGen            = delay(UUID.randomUUID())
    val accountCache       = SimpleCache.refBased(accRef)
    val failGen            = LowLvlErrorGen.random[F](0.1, 0.05)
    val faultyAccountCache = SimpleCache.errorProne(accountCache, failGen)
    val accountDb          = AccountDb.make(faultyAccountCache)
    val txCache            = SimpleCache.refBased(txRef)
    val faultyTxCache      = SimpleCache.errorProne(txCache, failGen)
    val txDb               = TxDb.make(faultyTxCache)
    AccountService.make(txIdGen, accountDb, txDb)

  private def makeService[F[_]: MonadBLError: Defer](
      accRef: Ref[F, AccountMap],
      txRef: Ref[F, TxMap]
  ): AccountService[F] =
    val txIdGen      = delay(UUID.randomUUID())
    val accountCache = SimpleCache.refBased(accRef)
    val accountDb    = AccountDb.make(accountCache)
    val txCache      = SimpleCache.refBased(txRef)
    val txDb         = TxDb.make(txCache)
    AccountService.make(txIdGen, accountDb, txDb)

  def runApp[F[_]: Ref.Make: Random: Defer: MonadBLError](): F[Unit] =
    import Constants.*
    for
      accRef <- Ref.of[F, AccountMap](accounts)
      txRef  <- Ref.of[F, TxMap](txs)
//    service = makeService(accRef, txRef)
      service = makeFaultyService(accRef, txRef)
      beforeUser1 <- service.getAccount(user1)
      beforeUser2 <- service.getAccount(user2)
      _           <- log(s"Before: $beforeUser1, $beforeUser2")
      _           <- service.makeTransfer(TxInfo(user1, user2, 10))
      afterUser1  <- service.getAccount(user1)
      afterUser2  <- service.getAccount(user2)
      _           <- log(s"After: $afterUser1, $afterUser2")
    yield ()
