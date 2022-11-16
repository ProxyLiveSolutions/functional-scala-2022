package com.onairentertainment.functional_scala_2022.account

import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError

/** Clean implementation. It contains only the business logic. */
private[account] class CleanImpl[F[_]](txIdGen: F[TxId], accountDb: AccountDb[F], txDb: TxDb[F])(using
    M: MonadBLError[F]
) extends AccountService[F]:
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
