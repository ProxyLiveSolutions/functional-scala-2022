package com.onairentertainment.functional_scala_2022.account

import com.onairentertainment.functional_scala_2022.account.BusinessLevelError.NotEnoughMoney

import java.util.UUID

type UserId     = UUID
type Money      = BigInt
type AccountMap = Map[UserId, Money]
type TxMap      = Map[TxId, TxInfo]
final case class Account(owner: UserId, balance: Money)

object Account:
  def withdraw(account: Account, amount: Money): Either[NotEnoughMoney, Account] =
    if (account.balance >= amount)
      Right(account.copy(balance = account.balance - amount))
    else
      Left(NotEnoughMoney(amount, account.balance))

  def deposit(account: Account, amount: Money): Account =
    account.copy(balance = account.balance + amount)
