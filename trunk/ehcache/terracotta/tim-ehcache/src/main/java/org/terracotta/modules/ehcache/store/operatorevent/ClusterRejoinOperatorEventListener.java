/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.operatorevent;

import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.api.TerracottaExtras;
import org.terracotta.cluster.OperatorEventLevel;
import org.terracotta.cluster.OperatorEventSubsystem;
import org.terracotta.modules.ehcache.event.TerracottaNodeImpl;

public class ClusterRejoinOperatorEventListener implements ClusterTopologyListener {
  private volatile boolean  clusterOnline = true;
  private final ClusterNode currentNode;

  public ClusterRejoinOperatorEventListener(org.terracotta.cluster.ClusterNode currentNode) {
    this.currentNode = new TerracottaNodeImpl(currentNode);
  }

  public void clusterOffline(ClusterNode node) {
    this.clusterOnline = false;
  }

  public void clusterOnline(ClusterNode node) {
    this.clusterOnline = true;
  }

  public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
    if (clusterOnline) {
      TerracottaExtras.fireOperatorEvent(OperatorEventLevel.INFO, OperatorEventSubsystem.TOOLKIT, oldNode.getId()
                                                                                            + " rejoined as "
                                                                                            + newNode.getId());
    }
  }

  public void nodeJoined(ClusterNode node) {
    //
  }

  public void nodeLeft(ClusterNode node) {
    if (this.currentNode.equals(node)) {
      this.clusterOnline = false;
    }
  }

}
