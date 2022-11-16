package com.onairentertainment.functional_scala_2022.tycl

import cats.~>

// FunctorK can be found in cats-tagless or other libraries for functional programming
// The definition as well as instance derivation
trait FunctorK[Alg[_[_]]]:
  def mapK[F[_], G[_]](alg: Alg[F])(fg: F ~> G): Alg[G]

object FunctorK:
  implicit class FunctorKOps[Alg[_[_]], F[_]](alg: Alg[F]):
    def mapK[G[_]](using F: FunctorK[Alg])(fg: F ~> G): Alg[G] = F.mapK(alg)(fg)
