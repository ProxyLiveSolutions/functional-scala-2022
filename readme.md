# Natural transformations in practice

## Table of content
* Problem definition
  * Problem + solution
    * Why?
    * When can NT help?
* What is natural transformation?
  * Scala 2.x / Scala 3.x
  * Category theory
* Specific examples
  * Migration between different IO-monads
  * Extraction of domain-unrelated code outside business logic.
  * Error-passing and error-handling
  * Retries
  * Managing execution context
* Links

## Problem definition
In every application there are multiple scopes of required features to be implemented at the same time, at the same code. Usually we can split it on functional and non-functional requirements.
Functional requirements are considered as the logic needed for implementing a business feature, non-functional requirements include something like error-handling, logging, management of execution contexts, security and so on.
And usually we don't want to have all this logic put in one place as all together it can make a big mess. _It is better to have logic split on smaller combinable and reusable parts_.
Scala as a programming language provides many tools for doing so and using natural transformations is one of them. 

## What is natural transformation?
But let's start with a definition and simple examples at first.

### Scala 2/3
In the Cats 🐈 library a natural transformation is represented by this type and its type alias:
```scala
// in FunctionK.scala
trait FunctionK[F[_], G[_]] {
  def apply[A](fa: F[A]): G[A] = ... // omitted
}

//in the `cats` package
type ~>[F[_], G[_]] = arrow.FunctionK[F, G]

// Thanks to polymorphic functions in Scala 3
type ~~>[F[_], G[_]] = [A] => F[A] => G[A]
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

  val forScala3Only: Vector ~~> Option = [A] => (va: Vector[A]) => va.headOption
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
The crucial part here we can see in definitions of the transformations above - we separate an inside value of a type `A` and an IO-monad.
We can't work with a value of `A` in the logic of a natural transformation as we know nothing about it there. This enforces **separation of concerns** - a design principle for separating a program into distinct sections.
Having this principle in mind I am going to show you how to use natural transformations for separating error handling from business logic.

## Sample application
In order to go further we need a problem to solve 🙂
Let's create one by creating a simple service `AccountService` that can transfer money between users' accounts.

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
And let's make this service error-prone. So in some cases it can fail while working with money.
For doing so I am going to implement an in-memory cache that randomly raises errors instead for working properly.
And use it to store user's money. In practice, it is not a good idea but for our case this is exactly what we need.

```scala
trait SimpleCache[F[_], K, V]:
  def get(key: K): F[Option[V]]
  def insert(key: K, value: V): F[InsertResult]
  def update(key: K, value: V): F[UpdateResult]

private final class ErrorProneCacheImpl[F[_], K, V](
    underlying: SimpleCache[F, K, V],
    errorGen: LowLvlErrorGen[F]
)(using
    M: MonadBLError[F]
) extends SimpleCache[F, K, V]:
  private def wrapWithError[A](fa: F[A]): F[A] = ... //omitted

  override def get(key: K): F[Option[V]]                 = wrapWithError(underlying.get(key))
  override def insert(key: K, value: V): F[InsertResult] = wrapWithError(underlying.insert(key, value))
  override def update(key: K, value: V): F[UpdateResult] = wrapWithError(underlying.update(key, value))
```

__TBD__

### Retries
__TBD__
Retries themselves
Gathering of retries' statistic with WriterT.

### Managing execution context
__TBD__

## Links

1. Definition of a natural transformation in wiki - https://en.wikipedia.org/wiki/Natural_transformation
2. Definition of non-functional requirement - https://en.wikipedia.org/wiki/Non-functional_requirement
3. Polymorphic functions in Scala 3 - https://docs.scala-lang.org/scala3/reference/new-types/polymorphic-function-types.html
4. Separation of concerns - https://en.wikipedia.org/wiki/Separation_of_concerns