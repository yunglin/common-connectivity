package tw.hyl.common.connectivity.exception

class ConnectivityException(msg: String, reason: Throwable = null) extends Exception(msg, reason)

class CircuitBreakerException(msg: String, reason: Throwable = null) extends ConnectivityException(msg, reason)

case class CircuitBreakerOpenException(msg: String) extends CircuitBreakerException(msg)

case class NotAvailableException(msg: String) extends ConnectivityException(msg)

