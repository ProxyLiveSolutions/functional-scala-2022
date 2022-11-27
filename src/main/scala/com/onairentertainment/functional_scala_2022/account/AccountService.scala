package com.onairentertainment.functional_scala_2022.account

import cats.{MonadError, ~>}
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError
import com.onairentertainment.functional_scala_2022.tycl.FunctorK

trait AccountService[F[_]]:
  /** Transfers money between accounts */
  def makeTransfer(tx: TxInfo): F[Unit]

  /** Gets account's info by the UserId */
  def getAccount(id: UserId): F[Account]

object AccountService:
  given FunctorK[AccountService] = new FunctorK[AccountService]:
    override def mapK[F[_], G[_]](
        alg: AccountService[F]
    )(
        fg: F ~> G
    ): AccountService[G] =
      new AccountService[G]:
        override def makeTransfer(tx: TxInfo): G[Unit] =
          fg(alg.makeTransfer(tx))
        override def getAccount(id: UserId): G[Account] =
          fg(alg.getAccount(id))

  def make[F[_]: MonadBLError](txIdGen: F[TxId], accountDb: AccountDb[F], txDb: TxDb[F]): AccountService[F] =
    new CleanImpl[F](txIdGen, accountDb, txDb)
