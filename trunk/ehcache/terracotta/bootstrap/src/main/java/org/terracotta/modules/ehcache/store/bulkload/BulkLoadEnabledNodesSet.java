/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import net.sf.ehcache.store.StoreListener;

import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BulkLoadEnabledNodesSet {

  private static final String             BULK_LOAD_NODES_SET_PREFIX = "__tc_bulk-load-nodes-set_for_cache_";

  private final ToolkitLogger             logger;
  private final ClusterInfo               clusterInfo;
  private final ToolkitSet<String>        bulkLoadEnabledNodesSet;
  private final ToolkitLock               clusteredLock;
  private final String                    name;
  private final CleanupOnNodeLeftListener cleanupOnNodeLeftListener;
  private final boolean                   loggingEnabled;

  private final StoreListener             listener;

  protected BulkLoadEnabledNodesSet(ToolkitInternal toolkit, String name, StoreListener listener) {
    this.name = name;
    this.clusterInfo = toolkit.getClusterInfo();
    this.listener = listener;

    bulkLoadEnabledNodesSet = toolkit.getSet(BULK_LOAD_NODES_SET_PREFIX + name, String.class);
    clusteredLock = bulkLoadEnabledNodesSet.getReadWriteLock().writeLock();

    logger = toolkit.getLogger(BulkLoadEnabledNodesSet.class.getName());

    cleanupOfflineNodes();
    cleanupOnNodeLeftListener = new CleanupOnNodeLeftListener(this, clusterInfo, toolkit);
    clusterInfo.addClusterListener(cleanupOnNodeLeftListener);
    this.loggingEnabled = BulkLoadConstants.isLoggingEnabled(toolkit.getProperties());
  }

  private static final String getIdForNode(ClusterNode node) {
    return node.getId();
  }

  private void debug(String msg) {
    List<String> nodes = new ArrayList(bulkLoadEnabledNodesSet);
    logger.info("['" + name + "'] " + msg + " [bulk-load enabled nodes: " + nodes + "]");
  }

  /**
   * Remove offline nodes from the nodes set
   */
  private void cleanupOfflineNodes() {
    clusteredLock.lock();
    try {

      Collection<ClusterNode> liveNodes = clusterInfo.getNodes();
      ArrayList<String> defunctNodes = new ArrayList<String>(bulkLoadEnabledNodesSet);

      if (loggingEnabled) {
        debug("Cleaning up offline nodes. Live nodes: " + liveNodes);
      }
      // remove live nodes
      for (ClusterNode node : liveNodes) {
        defunctNodes.remove(getIdForNode(node));
      }

      // clean up defunct nodes
      for (String nodeId : defunctNodes) {
        bulkLoadEnabledNodesSet.remove(nodeId);
      }

      if (loggingEnabled) {
        debug("Offline nodes cleanup complete");
      }
    } finally {
      clusteredLock.unlock();
    }
  }

  /**
   * Add the current node in the bulk-load enabled nodes set
   */
  public void addCurrentNode() {
    clusteredLock.lock();
    try {
      String currentNodeId = getIdForNode(clusterInfo.getCurrentNode());
      bulkLoadEnabledNodesSet.add(currentNodeId);
      if (loggingEnabled) {
        debug("Added current node ('" + currentNodeId + "')");
      }
    } finally {
      clusteredLock.unlock();
    }
  }

  /**
   * Remove the current node from the bulk-load enabled nodes set
   */
  public void removeCurrentNode() {
    clusteredLock.lock();
    try {
      removeNodeIdAndNotifyAll(getIdForNode(clusterInfo.getCurrentNode()));
    } finally {
      clusteredLock.unlock();
    }
  }

  /**
   * This method should be called under write lock
   */
  private void removeNodeIdAndNotifyAll(String nodeId) {
    bulkLoadEnabledNodesSet.remove(nodeId);

    if (loggingEnabled) {
      debug("Removed node ('" + nodeId + "'), going to signal all.");
    }
    // notify all waiters
    clusteredLock.getCondition().signalAll();
  }

  /**
   * Wait until the bulk-load enabled nodes set is empty
   */
  public void waitUntilSetEmpty() throws InterruptedException {
    clusteredLock.lock();
    try {
      while (true) {
        if (bulkLoadEnabledNodesSet.size() == 0) {
          break;
        }

        if (loggingEnabled) {
          debug("Waiting until bulk-load enabled nodes list is empty");
        }
        // wait until somebody removes from the nodes set
        clusteredLock.getCondition().await(10, TimeUnit.SECONDS);
        if (bulkLoadEnabledNodesSet.size() > 0) {
          cleanupOfflineNodes();
        }
      }
    } finally {
      clusteredLock.unlock();
    }
  }

  public boolean isBulkLoadEnabledInCluster() {
    clusteredLock.lock();
    try {
      boolean rv = (bulkLoadEnabledNodesSet.size() != 0);
      if (loggingEnabled) {
        debug("Is bulk-load enabled in cluster? " + rv);
      }
      return rv;
    } finally {
      clusteredLock.unlock();
    }
  }

  private static class CleanupOnNodeLeftListener implements ClusterListener {

    private final BulkLoadEnabledNodesSet nodesSet;
    private final ClusterNode             currentNode;

    private final ClusterInfo             clusterInfo;

    public CleanupOnNodeLeftListener(BulkLoadEnabledNodesSet nodesSet, ClusterInfo clusterInfo, ToolkitInternal toolkit) {
      this.nodesSet = nodesSet;
      this.clusterInfo = clusterInfo;
      this.currentNode = clusterInfo.getCurrentNode();
    }

    @Override
    public void onClusterEvent(ClusterEvent event) {
      switch (event.getType()) {
        case NODE_LEFT:
          handleNodeLeft(event);
          break;
        default:
          // not interested
          break;
      }
    }

    private void handleNodeLeft(ClusterEvent event) {
      String offlineNode = getIdForNode(event.getNode());
      nodesSet.logger.info("Received node left event for: " + offlineNode);
      if (getIdForNode(currentNode).equals(offlineNode)) {
        // nothing to do on "this" node left
        if (nodesSet.loggingEnabled) {
          nodesSet.logger.info("Ignoring node left of current node itself - " + offlineNode);
        }
        return;
      }
      if (!clusterInfo.areOperationsEnabled()) {
        // no-op when current node is offline already
        nodesSet.logger.warn("Ignoring node left of node: " + offlineNode + ", as current node is offline.");
        return;
      }
      nodesSet.clusteredLock.lock();
      try {
        boolean shouldSend = !nodesSet.bulkLoadEnabledNodesSet.isEmpty();

        nodesSet.removeNodeIdAndNotifyAll(offlineNode);

        if (shouldSend && nodesSet.bulkLoadEnabledNodesSet.isEmpty()) {
          nodesSet.listener.clusterCoherent(true);
        }

      } finally {
        nodesSet.clusteredLock.unlock();
      }
    }
  }

  public void disposeLocally() {
    clusterInfo.removeClusterListener(cleanupOnNodeLeftListener);
  }

}
