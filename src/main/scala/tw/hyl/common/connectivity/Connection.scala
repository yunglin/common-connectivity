package tw.hyl.common.connectivity

import java.util.UUID
import scala.concurrent.duration._
import scala.util.Try

trait Connection[+A] {

  lazy val _id = UUID.randomUUID().toString

  def id: String = _id

  def isAlive: Boolean = true

  def execute[B](body: A => Try[B]): Try[B]
}

object Connection {

  def apply[A](connection: A): Connection[A] = new SingleConnection(connection)

  def circuitBreak[A](
    connection: Connection[A],
    failLimit: Int = 5,
    retryDelay: Duration = 30.seconds): Connection[A] = {
    new CircuitBreaker(connection, failLimit, retryDelay)
  }
}

