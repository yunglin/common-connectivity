package tw.hyl.common.connectivity

import java.util.concurrent.locks.ReentrantLock
import scala.util.{Failure, Try}

private case class SingleConnection[A](connection: A) extends Connection[A] {

  /**
   * use lock instead of synchronized block because lock is 2x more efficient
   * in high contention situation.
   */
  private val lock = new ReentrantLock

  def execute[B](body: (A) => Try[B]): Try[B] = {
    try {
      lock.lock()
      body(connection)
    } catch {
      // just in case the body throws exception.
      case e: Exception => Failure(e)
    } finally {
      lock.unlock()
    }
  }
}
