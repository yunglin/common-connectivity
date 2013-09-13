package tw.hyl.common.connectivity

import tw.hyl.common.connectivity.exception.ConnectivityException
import org.testng.annotations.Test
import scala.util.{ Failure, Success }
import org.scalatest.testng.TestNGSuite


class SingleConnectionTest extends TestNGSuite {

  @Test
  def `test create a connection and execute command` {
    val conn = Connection("A")
    val ret = conn.execute {
      input => Success(input * 2)
    }

    assert(ret === Success("AA"))
  }

  @Test
  def `testexecute command and return failure` {
    val conn = Connection("A")
    val ret = conn.execute {
      input => Failure(new ConnectivityException("a error"))
    }

    assert(ret.isFailure)
    val thrown = intercept[ConnectivityException] {
      ret.get
    }
    assert(thrown.getMessage === "a error")
  }
}
