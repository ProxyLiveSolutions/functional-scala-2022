package com.onairentertainment.functional_scala_2022.simple

import cats.effect.{IOApp, LiftIO, IO as CatsIO}
import cats.~>
import zio.{Task, Unsafe, ZIO}
import zio.interop.catz.liftIOInstance

import scala.concurrent.Future
import zio.Runtime.default
import zio.Unsafe.{unsafe, unsafeCompat}

object IOExample extends IOApp.Simple:
  override def run: CatsIO[Unit] =
    val Future2Zio: Future ~> Task = new (Future ~> Task):
      override def apply[A](fa: Future[A]): Task[A] = ZIO.fromFuture(_ => fa)

    val Zio2Future: Task ~> Future = new (Task ~> Future):
      override def apply[A](fa: Task[A]): Future[A] =
        unsafeCompat { (instance: Unsafe) =>
          implicit val impl = instance
          default.unsafe.runToFuture(fa)
        }.future

    val Cats2Zio: CatsIO ~> Task = LiftIO.liftK[Task](liftIOInstance(runtime))

    val Future2Cats: Future ~> CatsIO = new (Future ~> CatsIO):
      override def apply[A](fa: Future[A]): CatsIO[A] = CatsIO.fromFuture(CatsIO(fa))

    val printHelloWorld = CatsIO(println("Hello World"))
    val roundtrip       = Cats2Zio andThen Zio2Future andThen Future2Cats
    roundtrip(printHelloWorld)
