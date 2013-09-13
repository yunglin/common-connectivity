package tw.hyl.common.connectivity

import java.util.concurrent.LinkedBlockingQueue
import scala.util.Try

private class ConnectionPool[A](factory: => A, size: Int) extends Connection[A] {

  lazy val pool = {
    val ret = new LinkedBlockingQueue[A]()
    for (i <- 0 until size) {
      ret.put(factory)
    }
    ret
  }

  def execute[B](body: (A) => Try[B]): Try[B] = {
    Try {
      pool.poll()
    }.flatMap {
      c =>
        {
          val ret = Try {
            body(c)
          }.flatten
          pool.put(c)
          ret
        }
    }
  }
}
