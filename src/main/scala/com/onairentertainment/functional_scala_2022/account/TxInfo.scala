package com.onairentertainment.functional_scala_2022.account

import java.util.UUID

type TxId = UUID
final case class Tx(id: TxId, info: TxInfo)
final case class TxInfo(from: UserId, to: UserId, amount: Money)
