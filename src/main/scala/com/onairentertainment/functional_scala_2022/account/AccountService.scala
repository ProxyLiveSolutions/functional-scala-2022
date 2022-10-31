package com.onairentertainment.functional_scala_2022.account

import cats.MonadError
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError

trait AccountService[F[_]]:
  /** Transfers money between accounts */
  def makeTransfer(tx: TxInfo): F[Unit]

  /** Gets account's info by the UserId*/
  def getAccount(id: UserId): F[Account]

object AccountService:
  private final class Impl[F[_]](txIdGen: F[TxId], accountDb: AccountDb[F], txDb: TxDb[F])(using M: MonadBLError[F])
      extends AccountService[F]:
    override def makeTransfer(info: TxInfo): F[Unit] =
      for
        fromAcc     <- accountDb.findAccount(info.from)
        toAcc       <- accountDb.findAccount(info.to)
        updatedFrom <- tryWithdraw(fromAcc, info.amount)
        updatedTo = Account.deposit(toAcc, info.amount)
        _  <- accountDb.updateAccount(updatedFrom)
        _  <- accountDb.updateAccount(updatedTo)
        id <- txIdGen
        _  <- txDb.saveTx(Tx(id, info))
      yield ()

    private def tryWithdraw(account: Account, required: Money): F[Account] =
      M.fromEither(Account.withdraw(account, required))

    override def getAccount(id: UserId): F[Account] = accountDb.findAccount(id)

  def make[F[_]: MonadBLError](txIdGen: F[TxId], accountDb: AccountDb[F], txDb: TxDb[F]): AccountService[F] =
    new Impl[F](txIdGen, accountDb, txDb)
