/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import org.terracotta.toolkit.Toolkit;
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

  private static volatile ToolkitLogger   LOGGER;
  private static final boolean            LOGGING_ENABLED            = BulkLoadConstants.isLoggingEnabled();
  private static final String             BULK_LOAD_NODES_SET_PREFIX = "__tc_bulk-load-nodes-set_for_cache_";
  private final ClusterInfo               clusterInfo;
  private final ToolkitSet<String>        bulkLoadEnabledNodesSet;
  private final ToolkitLock               clusteredLock;
  private final String                    name;
  private final CleanupOnNodeLeftListener cleanupOnNodeLeftListener;

  protected BulkLoadEnabledNodesSet(Toolkit toolkit, String name) {
    this.name = name;
    this.clusterInfo = toolkit.getClusterInfo();
    bulkLoadEnabledNodesSet = toolkit.getSet(BULK_LOAD_NODES_SET_PREFIX + name);
    clusteredLock = bulkLoadEnabledNodesSet.getReadWriteLock().writeLock();
    
    if (LOGGER == null) {
      LOGGER = ((ToolkitInternal) toolkit).getLogger(BulkLoadEnabledNodesSet.class.getName());
    }

    cleanupOfflineNodes();
    cleanupOnNodeLeftListener = new CleanupOnNodeLeftListener(this, clusterInfo, toolkit);
    clusterInfo.addClusterListener(cleanupOnNodeLeftListener);
  }

  private static final String getIdForNode(ClusterNode node) {
    return node.getId();
  }

  private void debug(String msg) {
    List<String> nodes = new ArrayList(bulkLoadEnabledNodesSet);
    LOGGER.info("['" + name + "'] " + msg + " [bulk-load enabled nodes: " + nodes + "]");
  }

  /**
   * Remove offline nodes from the nodes set
   */
  private void cleanupOfflineNodes() {
    clusteredLock.lock();
    try {

      Collection<ClusterNode> liveNodes = clusterInfo.getNodes();
      ArrayList<String> defunctNodes = new ArrayList<String>(bulkLoadEnabledNodesSet);

      if (LOGGING_ENABLED) {
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

      if (LOGGING_ENABLED) {
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
      if (LOGGING_ENABLED) {
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

    if (LOGGING_ENABLED) {
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

        if (LOGGING_ENABLED) {
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
      if (LOGGING_ENABLED) {
        debug("Is bulk-load enabled in cluster? " + rv);
      }
      return rv;
    } finally {
      clusteredLock.unlock();
    }
  }

  private static class CleanupOnNodeLeftListener implements ClusterListener {

    private volatile static ToolkitLogger LOG;

    private final BulkLoadEnabledNodesSet nodesSet;
    private final ClusterNode             currentNode;

    private final ClusterInfo             clusterInfo;

    public CleanupOnNodeLeftListener(BulkLoadEnabledNodesSet nodesSet, ClusterInfo clusterInfo, Toolkit toolkit) {
      this.nodesSet = nodesSet;
      this.clusterInfo = clusterInfo;
      this.currentNode = clusterInfo.getCurrentNode();
      if (LOG == null) {
        LOG = ((ToolkitInternal) toolkit).getLogger(CleanupOnNodeLeftListener.class.getName());
      }
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
      LOGGER.info("Received node left event for: " + offlineNode);
      if (getIdForNode(currentNode).equals(offlineNode)) {
        // nothing to do on "this" node left
        if (LOGGING_ENABLED) {
          LOGGER.info("Ignoring node left of current node itself - " + offlineNode);
        }
        return;
      }
      if (!clusterInfo.areOperationsEnabled()) {
        // no-op when current node is offline already
        LOG.warn("Ignoring node left of node: " + offlineNode + ", as current node is offline.");
        return;
      }
      nodesSet.clusteredLock.lock();
      try {
        nodesSet.removeNodeIdAndNotifyAll(offlineNode);
      } finally {
        nodesSet.clusteredLock.unlock();
      }
    }
  }

  public void disposeLocally() {
    clusterInfo.removeClusterListener(cleanupOnNodeLeftListener);
  }

}
