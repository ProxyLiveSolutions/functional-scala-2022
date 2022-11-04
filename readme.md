# Natural transformations in practice

## Table of content
* What is natural transformation?
  * Scala 2.x / Scala 3.x
  * Category theory
* Problem definition
  * Problem + solution
    * Why?
    * When can NT help?
* Specific examples
  * Migration between different IO-monads
  * Extraction of domain-unrelated code outside business logic.
  * Error-passing and error-handling
  * Retries
  * Managing execution context
* Links

## What is natural transformation?

### Scala 2/3
In the Cats 🐈 library a natural transformation is represented by this type and its type alias:
```scala
// in FunctionK.scala
trait FunctionK[F[_], G[_]]

//in the `cats` package
type ~>[F[_], G[_]] = arrow.FunctionK[F, G]
```

### Category theory

> __Natural transformation__ provides a way of transforming one functor into another while respecting the internal structure (i.e., the composition of morphisms) of the categories involved.


## Simple Examples

### Transformation of collections
The simplest example of using natural transformations is to convert different collections to each other:
```scala
  val Array2List: Array ~> List = new (Array ~> List):
    override def apply[A](fa: Array[A]): List[A] = fa.toList

  val List2Vector: List ~> Vector = new (List ~> Vector):
    override def apply[A](fa: List[A]): Vector[A] = fa.toVector

  val Vector2Option: Vector ~> Option = new (Vector ~> Option):
    override def apply[A](fa: Vector[A]): Option[A] = fa.headOption
```

After defining these transformations we can compose them as usual Scala functions:
```scala
val combined: Array ~> Option = Array2List andThen List2Vector andThen Vector2Option
```

### Migration between IO-monads
The previous example may seem too simple and useless. But it is also possible to use nat. transformations for converting different IO monads to each other.
```scala
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
```
This can be useful for the cases when a project is being migrated from one IO monad to another.

## Sample application
In order to further and to show how natural transformations can be used in more practical cases we need a problem to solve 🙂
Let's create one by creating a simple service `AccountService` that can transfer money between users' accounts. And let's make this service error-prone.
So in some cases it can fail while working with money.  

Here is its interface:

```scala
type TxId = UUID
final case class Tx(id: TxId, info: TxInfo)
final case class TxInfo(from: UserId, to: UserId, amount: Money)

trait AccountService[F[_]]:
  /** Transfers money between accounts */
  def makeTransfer(tx: TxInfo): F[Unit]
  
  /** Gets account's info by the UserId*/
  def getAccount(id: UserId): F[Account]
```


### Error-passing and error-handling
__TBD__

### Retries
__TBD__
Retries themselves
Gathering of retries' statistic with WriterT.

### Managing execution context
__TBD__

## Links

1. Definition of a natural transformation in wiki - https://en.wikipedia.org/wiki/Natural_transformation