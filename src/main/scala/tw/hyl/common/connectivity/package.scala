package tw.hyl.common.connectivity

/**
 * == Usage and Requirements: ==
 *
 * Connection must be stackable. ie.
 *
 * val conn: Connection[AWS] = new ConnectionBuilder[AWS]
 *   .withCircuitBreaker(failLimit=5, retryDelay = 30 seconds)
 *   .withLoadBalancer(
 *     provider = RoundRobin(() => List(nodes...))
 *   )
 *   .withConnectionPool(
 *     factory = Node => AWSClient
 *     size = 10
 *   )
 * .build
 *
 * what really happens here is that an execute take a Measurable(ARM) instance that will trace the execution
 * status (ie: Opened/Half-Opened/Closed, Lending/Available, Consecutive errors, Response Time)
 *
 *
 * == How to properly build a connection ==
 *
 * always build a Connection[T] from either a plain Connection or a ConnectionPool first. After that, we can
 * add either a CircuitBreaker or a LoadBalancer on the top of it.
 *
 * If we put a CircuitBreaker on the top of a ConnectionPool, if connections in the connection pools return
 * consecutive errors, it will shutdown the whole connection pool.
 *
 * If we put a LoadBalancer on the top of a ConnectionPool, multiple ConnectionPool instance will be initiated
 * using the information provided by the LoadBalancer.
 *
 * LoadBalancer can be either RoundRobin Load Balancer which every time you invoke it, it give you the next instance
 * in the line. If a connection instance has been shutdown by the CircuitBreaker, it will not be chosen from the
 * list because its isAlive flag is false.
 *
 * When Least Response Time LoadBalancer is used, it will wrap each Connection[T] with Measureable[T], when a
 * Connection execute a task, it will track the response time of each execution. Also, there is a random number
 * generator so that the LRT LB will randomly sample the response time of un-used Connection.
 *
 */
package object connection {

}
