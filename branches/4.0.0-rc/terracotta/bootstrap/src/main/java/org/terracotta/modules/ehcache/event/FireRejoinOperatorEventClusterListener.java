/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.monitoring.OperatorEventLevel;

public class FireRejoinOperatorEventClusterListener implements ClusterTopologyListener {
  private static final String  EHCACHE_OPERATOR_EVENT_APP_NAME = "ehcache";
  private volatile boolean     clusterOnline                   = true;
  private volatile ClusterNode currentNode;
  private final Toolkit        toolkit;

  public FireRejoinOperatorEventClusterListener(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkit = toolkitInstanceFactory.getToolkit();
  }

  @Override
  public void clusterOffline(ClusterNode node) {
    this.clusterOnline = false;
  }

  @Override
  public void clusterOnline(ClusterNode node) {
    this.clusterOnline = true;
    this.currentNode = new TerracottaNodeImpl(toolkit.getClusterInfo().getCurrentNode());
  }

  @Override
  public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
    if (clusterOnline) {
      toolkit.fireOperatorEvent(OperatorEventLevel.INFO, EHCACHE_OPERATOR_EVENT_APP_NAME,
                                oldNode.getId() + " rejoined as " + newNode.getId());
    }
  }

  @Override
  public void nodeJoined(ClusterNode node) {
    //
  }

  @Override
  public void nodeLeft(ClusterNode node) {
    if (this.currentNode.equals(node)) {
      this.clusterOnline = false;
    }
  }

}
