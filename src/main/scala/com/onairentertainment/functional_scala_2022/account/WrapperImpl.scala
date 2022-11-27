package com.onairentertainment.functional_scala_2022.account

import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.MonadBLError
import cats.syntax.show.*

private[account] class WrapperImpl[F[_]](
    underlying: AccountService[F],
    logErrors: String => F[Unit]
)(using M: MonadBLError[F])
    extends AccountService[F]:
  private def retry[A](fa: F[A]) = ??? // omitted
  private def logging[A](fa: F[A]): F[A] =
    M.onError(fa) { case error =>
      logErrors(show"AccountService has failed with an error[$error]")
    }
  override def makeTransfer(info: TxInfo): F[Unit] =
    logging(retry(underlying.makeTransfer(info)))
  override def getAccount(id: UserId): F[Account] =
    logging(retry(underlying.getAccount(id)))
