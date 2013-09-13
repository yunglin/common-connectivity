package tw.hyl.common.connectivity

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Success
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test
import tw.hyl.common.connectivity.exception.ConnectivityException

/**
 */
class ConnectionPoolTest extends TestNGSuite {

  @Test
  def `testCreate a single instance connection pool and execute command` {

    val counter = new AtomicInteger(1)
    val conn = new ConnectionPool[Int](counter.getAndIncrement, 1)

    // because we reuse same connection, the number will remain unchanged.
    assert(conn.execute { input => Success(input * 2) } === Success(2))
    assert(conn.execute { input => Success(input * 2) } === Success(2))
  }

  @Test
  def `test create two instances connection pool and execute command` {

    val counter = new AtomicInteger(1)
    val conn = new ConnectionPool[Int](counter.getAndIncrement, 2)

    assert(conn.execute { input => Success(input * 2) } === Success(2))
    assert(conn.execute { input => Success(input * 2) } === Success(4))
    assert(conn.execute { input => Success(input * 2) } === Success(2))
    assert(conn.execute { input => Success(input * 2) } === Success(4))
  }

  @Test
  def `test recycle connection after error occurs` {

    val counter = new AtomicInteger(1)
    val conn = new ConnectionPool[Int](counter.getAndIncrement, 2)

    assert(conn.execute { input => Success(input * 2) } === Success(2))
    assert(conn.execute { input => Success(input * 2) } === Success(4))
    assert(conn.execute { input => throw new ConnectivityException("oops") }.isFailure)
    assert(conn.execute { input => throw new ConnectivityException("oops") }.isFailure)
    assert(conn.execute { input => Success(input * 2) } === Success(2))
    assert(conn.execute { input => Success(input * 2) } === Success(4))
  }

}
