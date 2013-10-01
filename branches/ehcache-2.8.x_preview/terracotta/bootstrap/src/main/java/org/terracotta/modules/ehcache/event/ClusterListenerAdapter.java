/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterEvent.Type;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.internal.cluster.OutOfBandClusterListener;

public class ClusterListenerAdapter implements OutOfBandClusterListener {
  private static final String           EHCACHE_TERRACOTTA_PACKAGE_NAME = "net.sf.ehcache.terracotta";
  private final ClusterTopologyListener topologyListener;
  private volatile TerracottaNodeImpl   currentNode;
  private final ClusterInfo             cluster;

  public ClusterListenerAdapter(ClusterTopologyListener topologyListener, ClusterInfo cluster) {
    this.topologyListener = topologyListener;
    this.cluster = cluster;
  }

  @Override
  public void onClusterEvent(org.terracotta.toolkit.cluster.ClusterEvent event) {
    switch (event.getType()) {
      case NODE_JOINED:
        if (currentNode == null) {
          currentNode = new TerracottaNodeImpl(cluster.getCurrentNode());
        }
        topologyListener.nodeJoined(new TerracottaNodeImpl(event.getNode()));
        break;
      case NODE_LEFT:
        topologyListener.nodeLeft(new TerracottaNodeImpl(event.getNode()));
        break;
      case OPERATIONS_DISABLED:
        topologyListener.clusterOffline(new TerracottaNodeImpl(event.getNode()));
        break;
      case OPERATIONS_ENABLED:
        topologyListener.clusterOnline(new TerracottaNodeImpl(event.getNode()));
        break;
      case NODE_REJOINED:
        TerracottaNodeImpl oldNode = currentNode;
        currentNode = new TerracottaNodeImpl(event.getNode());
        topologyListener.clusterRejoined(oldNode, currentNode);
        break;
      case NODE_ERROR:
        // not bubbled upto ehcache layer yet
        break;
    }
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

  /**
   * use out-of-band for listeners in ehcache terracotta package for NODE_LEFT events (used for rejoin) and OPS_DISABLED
   * event
   */
  @Override
  public boolean useOutOfBandNotification(ClusterEvent event) {
    if (topologyListener.getClass().getName().startsWith(EHCACHE_TERRACOTTA_PACKAGE_NAME)
        && (event.getType() == Type.NODE_LEFT || event.getType() == Type.OPERATIONS_DISABLED)) { return true; }
    return false;
  }

}
