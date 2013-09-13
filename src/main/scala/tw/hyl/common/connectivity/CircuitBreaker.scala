package tw.hyl.common.connectivity

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.compat.Platform._
import scala.concurrent.duration.Duration
import scala.util.{ Success, Failure, Try }
import tw.hyl.common.connectivity.exception.CircuitBreakerOpenException

/**
 *
 *
 * @param delegate
 * @param failureLimit
 * @param retryDelay
 * @tparam A
 */
private case class CircuitBreaker[A](delegate: Connection[A],
                                     failureLimit: Int,
                                     retryDelay: Long) extends Connection[A] {

  def this(delegate: Connection[A],
           failureLimit: Int,
           retryDelay: Duration) = this(delegate, failureLimit, retryDelay.toMillis)

  // TODO: rewrite with State Monad
  @volatile
  private var currentState: State = new Closed(this)

  def reset: Unit = {
    synchronized {
      currentState = currentState match {
        // special case occurred when multiple requests are made to a HalfOpen
        // state and some fails and some succeeds. The failed ones will set
        // state to Open and then later the succeeded ones will set to Closed.
        // If this happens, we will ignores the 'reset' and make the circuit
        // breaker stay in Open.
        case o: Open[A] => o
        case _       => new Closed(this)
      }
    }
  }

  def open: Unit = {
    synchronized {
      currentState = currentState match {
        case o: Open[A] => o
        case _       => new Open(this, currentTime, retryDelay)
      }
    }
  }

  def halfOpen: Unit = {
    synchronized {
      currentState = currentState match {
        case h: HalfOpen[A] => h
        case _           => new HalfOpen(this)
      }
    }
  }

  override def isAlive: Boolean = currentState.isAlive

  def execute[B](body: (A) => Try[B]): Try[B] = {
    currentState.execute(delegate.execute(body))
  }
}

private[this] sealed trait State {

  def execute[A](body: => Try[A]): Try[A]

  def isAlive: Boolean
}

private[this] case class Closed[T](cb: CircuitBreaker[T]) extends State {

  val failureCount = new AtomicInteger()

  override def isAlive: Boolean = true

  def execute[A](body: => Try[A]): Try[A] = {
    val ret = body
    if (ret.isFailure && failureCount.incrementAndGet() == cb.failureLimit) {
      cb.halfOpen
    }
    ret
  }
}

private[this] case class Open[T](cb: CircuitBreaker[T], firstFailure: Long, retryDelay: Long) extends State {

  private var lastRetry = firstFailure

  private val lock = new ReentrantReadWriteLock()
  private val readLock = lock.readLock()
  private val writeLock = lock.writeLock()

  override def isAlive: Boolean = {
    return lastRetry + retryDelay >= currentTime
  }

  override def execute[A](body: => Try[A]): Try[A] = {
    readLock.lock()
    val ret: Try[A] = if (isAlive == false) {
      Failure(new CircuitBreakerOpenException("Circuit Breaker is in Open state."))
    } else {
      try {
        readLock.unlock()
        writeLock.lock()
        if (isAlive == false) {
          Failure(new CircuitBreakerOpenException("Circuit Breaker is in Open state."))
        } else {
          body match {
            case s: Success[A] => { cb.reset; s }
            case f => {
              lastRetry = currentTime
              Failure(new CircuitBreakerOpenException("Retry failed. Connection remains in Open state."))
            }
          }
        }
      } finally {
        writeLock.unlock()
        readLock.lock()
      }
    }
    readLock.unlock()
    ret
  }
}

private[this] case class HalfOpen[T](cb: CircuitBreaker[T]) extends State {

  override def execute[A](body: => Try[A]): Try[A] = {
    body match {
      case s: Success[A] => {
        cb.reset; s
      }
      case f: Failure[A] => {
        cb.open; f
      }
    }
  }

  override def isAlive: Boolean = true

}
