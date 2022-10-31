package com.onairentertainment.functional_scala_2022.account

import cats.Show
import com.onairentertainment.functional_scala_2022.cache.LowLevelError

sealed trait BusinessLevelError

object BusinessLevelError:
  import cats.MonadError
  type MonadBLError[F[_]] = MonadError[F, BusinessLevelError]
  final case class NotEnoughMoney(required: Money, current: Money) extends BusinessLevelError
  final case class AccountNotFound(userId: UserId)                 extends BusinessLevelError
  final case class TxAlreadyExists(txId: TxId)                     extends BusinessLevelError
  final case class LowLevel(error: LowLevelError)                  extends BusinessLevelError

  given Show[BusinessLevelError] = Show.fromToString
