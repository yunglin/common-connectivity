package tw.hyl.common.connectivity

import scala.concurrent.duration._

case class ConnectionBuilder[A](
    conn: Option[Connection[A]] = None,
    circuitBreaker: Boolean = false,
    failureLimit: Int = 5,
    retryDelay: Duration = 30.seconds) {

  def withCircuitBreaker(
    failureLimit: Int = 5,
    retryDelay: Duration = 30.seconds): ConnectionBuilder[A] = {
    this.copy(circuitBreaker = true, failureLimit = failureLimit, retryDelay = retryDelay)
  }

  def withConnection[B](conn: B): ConnectionBuilder[B] = {

    return new ConnectionBuilder[B](
      conn = Some(Connection(conn)),
      circuitBreaker = this.circuitBreaker,
      failureLimit = this.failureLimit,
      retryDelay = this.retryDelay
    )
  }

  def withConnectionPool[B](factory: => B, size: Int): ConnectionBuilder[B] = {

    return new ConnectionBuilder[B](
      conn = Some(new ConnectionPool(factory, size)),
      circuitBreaker = this.circuitBreaker,
      failureLimit = this.failureLimit,
      retryDelay = this.retryDelay
    )

  }

  def build: Connection[A] = {
    if (conn.isEmpty) {
      throw new IllegalArgumentException("Neither Connection nor ConnectionPool is specified!")
    }
    var ret = conn.get
    if (circuitBreaker) {
      ret = Connection.circuitBreak(ret, failLimit = failureLimit, retryDelay = retryDelay)
    }
    return ret
  }

}
