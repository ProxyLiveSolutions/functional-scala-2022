package com.onairentertainment.functional_scala_2022

import cats.~>
import cats.MonadError
import cats.mtl.Tell

object ErrorGatherer:
  def gatherErrorsK[F[_], E](using T: Tell[F, List[E]], M: MonadError[F, E]): F ~> F = new (F ~> F):
    override def apply[A](fa: F[A]): F[A] = M.onError(fa) { case e =>
      T.tell(List(e))
    }
