/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.OperatorEventLevel;
import org.terracotta.toolkit.internal.ToolkitInternal;

public class FireRejoinOperatorEventClusterListener implements ClusterTopologyListener {
  private static final String   EHCACHE_OPERATOR_EVENT_APP_NAME = "ehcache";
  private volatile boolean      clusterOnline                   = true;
  private final ClusterNode     currentNode;
  private final ToolkitInternal toolkitInternal;

  public FireRejoinOperatorEventClusterListener(ToolkitInstanceFactory toolkitInstanceFactory) {
    Toolkit toolkit = toolkitInstanceFactory.getToolkit();
    this.currentNode = new TerracottaNodeImpl(toolkit.getClusterInfo().waitUntilNodeJoinsCluster());
    this.toolkitInternal = (ToolkitInternal) toolkit;
  }

  public void clusterOffline(ClusterNode node) {
    this.clusterOnline = false;
  }

  public void clusterOnline(ClusterNode node) {
    this.clusterOnline = true;
  }

  public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
    if (clusterOnline) {
      toolkitInternal.fireOperatorEvent(OperatorEventLevel.INFO, EHCACHE_OPERATOR_EVENT_APP_NAME, oldNode.getId()
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
