/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.cluster.ClusterEvent;
import org.terracotta.cluster.ClusterEvent.Type;
import org.terracotta.cluster.OutOfBandClusterListener;

public class ClusterListenerAdapter implements OutOfBandClusterListener {
  private static final String           EHCACHE_TERRACOTTA_PACKAGE_NAME = "net.sf.ehcache.terracotta";

  private final ClusterTopologyListener topologyListener;

  public ClusterListenerAdapter(ClusterTopologyListener topologyListener) {
    this.topologyListener = topologyListener;
  }

  public void nodeJoined(ClusterEvent event) {
    topologyListener.nodeJoined(new TerracottaNodeImpl(event.getNode()));
  }

  public void nodeLeft(ClusterEvent event) {
    topologyListener.nodeLeft(new TerracottaNodeImpl(event.getNode()));
  }

  public void operationsDisabled(ClusterEvent event) {
    topologyListener.clusterOffline(new TerracottaNodeImpl(event.getNode()));
  }

  public void operationsEnabled(ClusterEvent event) {
    topologyListener.clusterOnline(new TerracottaNodeImpl(event.getNode()));
  }

  /**
   * use out-of-band for listeners in ehcache terracotta package for NODE_LEFT events (used for rejoin) and OPS_DISABLED
   * event
   */
  public boolean useOutOfBandNotification(ClusterEvent event) {
    if (topologyListener.getClass().getName().startsWith(EHCACHE_TERRACOTTA_PACKAGE_NAME)
        && (event.getType() == Type.NODE_LEFT || event.getType() == Type.OPERATIONS_DISABLED)) { return true; }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((topologyListener == null) ? 0 : topologyListener.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ClusterListenerAdapter other = (ClusterListenerAdapter) obj;
    if (topologyListener == null) {
      if (other.topologyListener != null) return false;
    } else if (!topologyListener.equals(other.topologyListener)) return false;
    return true;
  }

}
