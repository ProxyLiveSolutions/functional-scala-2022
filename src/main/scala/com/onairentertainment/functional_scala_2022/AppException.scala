package com.onairentertainment.functional_scala_2022

import AppException.*
import com.onairentertainment.functional_scala_2022.account.BusinessLevelError

import scala.util.control.NoStackTrace

private final case class AppException(error: BusinessLevelError) extends Exception(msg(error)) with NoStackTrace

object AppException:
  import cats.syntax.show.*
  def msg(error: BusinessLevelError): String = show"Business level error: $error"
