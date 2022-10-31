package com.onairentertainment.functional_scala_2022

import com.onairentertainment.functional_scala_2022.account.*
import java.util.UUID

object Constants:
  val user1: UserId        = UUID.fromString("f545d6b7-15c3-42ae-be40-903729c5525b")
  val user2: UserId        = UUID.fromString("84705965-e076-4e5a-8a73-4cf2b20da4b1")
  val accounts: AccountMap = Map.from(List(user1 -> BigInt(10), user2 -> BigInt(20)))
  val txs: TxMap           = Map.empty
