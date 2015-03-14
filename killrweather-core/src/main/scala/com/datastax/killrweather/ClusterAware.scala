package com.datastax.killrweather

import akka.actor.{ActorLogging, Actor}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

/**
 * Creates the [[Cluster]] and does any cluster lifecycle handling
 * aside from join and leave so that the implenting applications can
 * customize when this is done.
 *
 * Implemented by [[ClusterAwareNodeGuardian]].
 */
class ClusterAware extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  /** subscribe to cluster changes, re-subscribe when restart. */
  override def preStart(): Unit = cluster.subscribe(self, classOf[ClusterDomainEvent])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive : Actor.Receive = {
    case ClusterMetricsChanged(forNode) =>
      log.info("Cluster metrics update:")
      forNode foreach (m => log.info("{}", m))
    case MemberUp(member) =>
      log.info("Member {} joined cluster.", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent => // ignore
  }
}
