package com.onairentertainment.functional_scala_2022.cache

/** These errors emulate low level problems that can happen while working with a read DB */
sealed trait LowLevelError

object LowLevelError:
  /** Recoverable error */
  case object DbError extends LowLevelError

  /** Unrecoverable error */
  case object DbCriticalError extends LowLevelError
