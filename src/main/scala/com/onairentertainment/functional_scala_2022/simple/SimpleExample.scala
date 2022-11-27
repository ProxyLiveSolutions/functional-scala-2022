package com.onairentertainment.functional_scala_2022.simple

import cats.~>
import cats.arrow.FunctionK

import java.util
import scala.collection.mutable

object SimpleExample:
  val Array2List: Array ~> List = new (Array ~> List):
    override def apply[A](fa: Array[A]): List[A] = fa.toList

  val List2Vector: List ~> Vector = new (List ~> Vector):
    override def apply[A](fa: List[A]): Vector[A] = fa.toVector

  val Vector2Option: Vector ~> Option = new (Vector ~> Option):
    override def apply[A](fa: Vector[A]): Option[A] = fa.headOption

  val combined: Array ~> Option = Array2List `andThen` List2Vector
    `andThen` Vector2Option

  // Thanks to polymorphic functions in Scala 3
  type ~~>[F[_], G[_]] = [A] => F[A] => G[A]
  val forScala3Only: Vector ~~> Option = [A] => (va: Vector[A]) => va.headOption

  def main(args: Array[String]): Unit =
    val array1  = Array(1, 2, 3)
    val result1 = combined(array1)
    println(s"result1: $result1")

    val array2  = Array.empty[Int]
    val result2 = combined(array2)
    println(s"result2: $result2")
