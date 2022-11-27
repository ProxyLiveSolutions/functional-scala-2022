package com.onairentertainment.functional_scala_2022.simple

import cats.~>

object Intuition {
  class Class[F[_], G[_]]:
    // Polymorphic A without context and type bounds
    def function[A](fa: F[A]): G[A] = ???

  // ... is the same as this natural transformation
  def natTr[F[_], G[_]]: F ~> G = ???
}
