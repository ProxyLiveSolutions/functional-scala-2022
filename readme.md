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

And let's make this service faulty. So in some cases it can fail while working with money.
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
## Additional abstractions

But before continue we need to introduce two additional type classes - `Sleep[F[_]]` and `FunctorK[F[_[_]]]`.

`Sleep[F[_]]` is a very simple type class that allows us to stop execution of a program for some time.
We need it to avoid using `Temporal[F[_]]`.
```scala
trait Sleep[F[_]]:
  def sleep(duration: FiniteDuration): F[Unit]

object Sleep:
  def apply[F[_]](using S: Sleep[F]): Sleep[F] = S

  given sleepFromTemporal[F[_]: Temporal]: Sleep[F] =
    (duration: FiniteDuration) => Temporal[F].sleep(duration)
```

`FunctorK[F[_[_]]]` allows us to apply natural transformations to algebras. In other words - to services like `AccountService[F]`.
We will see how to use it a bit later.
```scala
trait FunctorK[Alg[_[_]]]:
  def mapK[F[_], G[_]](alg: Alg[F])(fg: F ~> G): Alg[G]
```
And its implementation for `AccountService[F]`
```scala
  given FunctorK[AccountService] = new FunctorK[AccountService]:
    override def mapK[F[_], G[_]](alg: AccountService[F])(fg: F ~> G): AccountService[G] = new AccountService[G]:
      override def makeTransfer(tx: TxInfo): G[Unit]  = fg(alg.makeTransfer(tx))
      override def getAccount(id: UserId): G[Account] = fg(alg.getAccount(id)) 
```
As you see, here calls from one implementation of `AccountService` are wrapped in a call of the natural transformation `fg`.

## Error-handling
Now when we have an error-prone service and all abstractions we need. It's time to deal with these errors. By retrying and logging them.
What options do we have for that? There are three of them:
* Implement all logic in one place 😨
* Introduce wrapper implementations - Java way 😐 
* Use natural transformations - Scala way 😎🤘

The first option is out of discussion - no way we are going to spend time to discuss it. Just imagine the worst case implementation of entangled code of business logic together with error handling, logging and ad-hoc retries 😄

The second option is a good old Java way of reimplementing interfaces by adding logging/error handling/other features to an underlying implementation.
```scala
private[account] class WrapperImpl[F[_]](underlying: AccountService[F], logErrors: String => F[Unit])(using
    M: MonadBLError[F]
) extends AccountService[F]:
  override def makeTransfer(info: TxInfo): F[Unit] = logging(retry(underlying.makeTransfer(info)))
  override def getAccount(id: UserId): F[Account]  = logging(retry(underlying.getAccount(id)))
  private def logging[A](fa: F[A]): F[A] = M.onError(fa) { case error =>
    logErrors(show"AccountService has failed with an error[$error]")
  }
  private def retry[A](fa: F[A]) = ??? // omitted
```
As you see, we just call `makeTransfer(..)` and `getAccount(..)` of `underlying` and wrapping results in `logging(..)` and `retry(..)`.
It works but in Scala we can do better. As you see in the definition of `def logging[A](fa: F[A]): F[A]` above we don't really care about a value inside `F[A]` here.
We need only an error from `F[A]`. This is a typical example of the use case for natural transformation. So let's do it and implement logging and retries as natural transformations.


### Retries
For implementing simple retries we are using `MonadError[F, E]` for getting errors from `F[_]` and `Sleep[F]` - for sleeping between attempts 😴
```scala
object ErrorRetry:
  private val SleepTime = 10.milli
  private val Attempts  = 5
  def simpleRetryK[F[_], E](using ME: MonadError[F, E], S: Sleep[F]): F ~> F = new (F ~> F):
    override def apply[A](fa: F[A]): F[A] =
      def internal(remain: Int): F[A] =
        ME.handleErrorWith(fa) { err =>
          if (remain <= 0)
            ME.raiseError(err)
          else
            S.sleep(SleepTime) >> internal(remain - 1)
        }
      internal(Attempts)
```
### Logging
For logging let's use the same implementation but defined as `F ~> F`
```scala
object ErrorLogger:
  def logErrorsK[F[_], E: Show](serviceName: String, log: String => F[Unit])(using ME: MonadError[F, E]): F ~> F =
    new (F ~> F):
      override def apply[A](fa: F[A]): F[A] = fa.onError { case error =>
        log(show"$serviceName has failed with an error[$error]")
      }
```

### Wiring
Now it is time to wire everything together and check how it works.
```scala
val logErrorsK = ErrorLogger.logErrorsK[F, BusinessLevelError]("AccountService", log)
val retryK     = ErrorRetry.simpleRetryK[F, BusinessLevelError]
for
  accRef <- Ref.of[F, AccountMap](accounts)
  txRef  <- Ref.of[F, TxMap](txs)
  //      (accCache, txCache) = makeCaches(accRef, txRef)
  (accCache, txCache) = makeFaultyCaches(accRef, txRef)
  service = makeService(accCache, txCache) // Make an instance of the service
    .mapK(logErrorsK) // Wraps the service with logging
    .mapK(retryK)     // Wraps the service with retries
  beforeUser1 <- service.getAccount(user1)
  beforeUser2 <- service.getAccount(user2)
  _           <- log(s"Before: $beforeUser1, $beforeUser2")
  _           <- service.makeTransfer(TxInfo(user1, user2, 10))
  afterUser1  <- service.getAccount(user1)
  afterUser2  <- service.getAccount(user2)
  _           <- log(s"After: $afterUser1, $afterUser2")
yield ()
```
Output
```scala
AccountService has failed with an error[LowLevel(DbError)]
AccountService has failed with an error[LowLevel(DbError)]
Before: Account(f545d6b7-15c3-42ae-be40-903729c5525b,10), Account(84705965-e076-4e5a-8a73-4cf2b20da4b1,20)
AccountService has failed with an error[LowLevel(DbError)]
AccountService has failed with an error[LowLevel(DbError)]
AccountService has failed with an error[LowLevel(DbCriticalError)]
After: Account(f545d6b7-15c3-42ae-be40-903729c5525b,0), Account(84705965-e076-4e5a-8a73-4cf2b20da4b1,30)
```
## Conclusion
Natural transformation is another abstraction available in Scala for a developer to help with separation of logic on different blocks.
Like in examples above we can use information about errors from IO-monad to implement different ways of error handling without changing the business logic.

## Links

1. Definition of a natural transformation in wiki - https://en.wikipedia.org/wiki/Natural_transformation
2. Definition of non-functional requirement - https://en.wikipedia.org/wiki/Non-functional_requirement
3. Polymorphic functions in Scala 3 - https://docs.scala-lang.org/scala3/reference/new-types/polymorphic-function-types.html
4. Separation of concerns - https://en.wikipedia.org/wiki/Separation_of_concerns