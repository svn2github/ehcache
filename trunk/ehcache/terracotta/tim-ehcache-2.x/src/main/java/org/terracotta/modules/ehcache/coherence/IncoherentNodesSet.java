/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import org.terracotta.cluster.ClusterEvent;
import org.terracotta.cluster.ClusterInfo;
import org.terracotta.cluster.ClusterListener;
import org.terracotta.cluster.ClusterLogger;
import org.terracotta.cluster.ClusterNode;
import org.terracotta.cluster.TerracottaClusterInfo;
import org.terracotta.cluster.TerracottaLogger;
import org.terracotta.cluster.TerracottaProperties;
import org.terracotta.collections.ConcurrentDistributedMap;
import org.terracotta.locking.LockType;
import org.terracotta.locking.strategy.HashcodeLockStrategy;
import org.terracotta.modules.ehcache.store.ClusteredStore;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IncoherentNodesSet implements CacheCoherence, ClusterListener {

  private static final ClusterLogger                LOGGER         = new TerracottaLogger(
                                                                                          IncoherentNodesSet.class
                                                                                              .getName());

  private static final boolean                      DEBUG          = new TerracottaProperties()
                                                                       .getBoolean(CacheCoherence.LOGGING_ENABLED_PROPERTY,
                                                                                   false);

  private static final Object                       SENTINEL_VALUE = new Object();

  private final ConcurrentMap<String, Object>       incoherentNodes;
  private final String                              cacheName;

  private volatile transient ClusterInfo            clusterInfo;
  private volatile transient AtomicBoolean          coherentLocally;
  private volatile transient ReentrantReadWriteLock readWriteLock;
  private transient Object                          nodeJoinedEventListenerSync;
  private final ClusteredStore                      clusteredStore;

  public IncoherentNodesSet(String name, ClusteredStore clusteredStore) {
    this.cacheName = name;
    this.clusteredStore = clusteredStore;
    incoherentNodes = new ConcurrentDistributedMap<String, Object>(LockType.WRITE, new HashcodeLockStrategy(), 1);
    init();
  }

  public void loadReferences() {
    debug("loadReferences()");
    incoherentNodes.getClass();
  }

  // on-load method
  public void init() {
    debug("init() start");
    this.clusterInfo = new TerracottaClusterInfo();

    if (clusterInfo == null) { throw new RuntimeException("TerracottaCluster is not injected."); }
    this.nodeJoinedEventListenerSync = new Object();
    readWriteLock = new ReentrantReadWriteLock();
    // initialize to correct value when faulting in
    coherentLocally = new AtomicBoolean(queryIsNodeCoherent());
    clusterInfo.addClusterListener(this);

    // need to verify and sanitize the list of incoherent nodes.
    // look at https://jira.terracotta.org/jira/browse/EHCTERR-2 for more details
    cleanIncoherentNodes(this.clusterInfo.getClusterTopology().getNodes());

    debug("init done()");
  }

  // write autolocked in config
  private synchronized void cleanIncoherentNodes(Collection<ClusterNode> nodes) {
    Set<String> nodesToRemoveFromIncoherentNodes = new HashSet<String>(this.incoherentNodes.keySet());
    for (ClusterNode node : nodes) {
      nodesToRemoveFromIncoherentNodes.remove(node.getId());
    }

    if (nodesToRemoveFromIncoherentNodes.size() > 0) {
      LOGGER.info("Sanitizing incoherent nodes set: need to remove defunct nodes: " + nodesToRemoveFromIncoherentNodes
                  + ", incoherent nodes: " + incoherentNodes.keySet());
      for (String nodeID : nodesToRemoveFromIncoherentNodes) {
        removeIncoherentNode(nodeID);
      }
    }
  }

  public boolean isClusterOnline() {
    return clusterInfo.areOperationsEnabled();
  }

  public void dispose() {
    this.clusterInfo.removeClusterListener(this);
  }

  public void acquireReadLock() {
    this.readWriteLock.readLock().lock();
  }

  public void acquireWriteLock() {
    this.readWriteLock.writeLock().lock();
  }

  public void releaseReadLock() {
    this.readWriteLock.readLock().unlock();
  }

  public void releaseWriteLock() {
    this.readWriteLock.writeLock().unlock();
  }

  // write auto-locked in config
  private synchronized boolean queryIsNodeCoherent() {
    final String currentNodeId = getCurrentNodeId();
    debug("queryIsNodeCoherent(): currentNode: " + currentNodeId);
    return !incoherentNodes.containsKey(currentNodeId);
  }

  // write autolocked in config
  private synchronized boolean gotoIncoherentMode() {
    final String currentNodeId = getCurrentNodeId();
    debug("gotoIncoherentMode(): Going incoherent - currentNode: " + currentNodeId);
    if (incoherentNodes.putIfAbsent(currentNodeId, SENTINEL_VALUE) == null) {
      debug("gotoIncoherentMode(): Added currentNode '" + currentNodeId + "' to incoherent nodes set");
      return true;
    } else {
      debug("gotoIncoherentMode(): currentNode '" + currentNodeId + "' already present in incoherent nodes set");
    }
    return false;
  }

  private void gotoCoherentMode() {
    String currentNodeId = getCurrentNodeId();
    debug("gotoCoherentMode(): Going to coherent mode: " + currentNodeId);
    removeIncoherentNode(currentNodeId);
    debug("gotoCoherentMode(): Going to coherent mode: " + currentNodeId + " done.");
  }

  // write autolocked in config
  private synchronized boolean removeIncoherentNode(String nodeID) {
    debug("removeIncoherentNode(): Going to remove nodeId from incoherent-nodes set: " + nodeID);
    if (incoherentNodes.remove(nodeID) != null) {
      this.notifyAll();
      return true;
    } else {
      debug("removeIncoherentNode(): Node id not present in incoherent nodes set: " + nodeID);
    }
    debug("removeIncoherentNode() done");
    return false;
  }

  // write autolocked in config
  public synchronized void waitUntilClusterCoherent() {
    while (incoherentNodes.size() > 0) {
      try {
        if (DEBUG) {
          debug("waitUntilClusterCoherent(): Going to wait until coherent cluster-wide");
        }
        this.wait();
        if (DEBUG) {
          debug("waitUntilClusterCoherent(): Got notified after removing incoherent node");
        }
      } catch (InterruptedException e) {
        return;
      }
    }
    debug("waitUntilClusterCoherent() done");
    return;
  }

  // read autolocked in config
  public synchronized boolean isClusterCoherent() {
    if (DEBUG) {
      debug("isClusterCoherent()");
    }
    return incoherentNodes.size() == 0;
  }

  // read autolocked in config
  public synchronized boolean isFirstIncoherent() {
    return incoherentNodes.size() == 1;
  }

  // returns true if this node is coherent
  // other nodes might still be incoherent
  public boolean isNodeCoherent() {
    return coherentLocally.get();
  }

  public void setNodeCoherent(boolean coherent) {
    if (coherent) {
      if (coherentLocally.compareAndSet(false, true)) {
        this.gotoCoherentMode();
      } // else already in coherent mode, no-op
    } else {
      if (coherentLocally.compareAndSet(true, false)) {
        this.gotoIncoherentMode();
      } // else already in incoherent mode, no-op
    }
  }

  private String getCurrentNodeId() {
    if (clusterInfo.isNodeJoined()) {
      return clusterInfo.getCurrentNode().getId();
    } else {
      waitUntilNodeJoinsCluster();
      return clusterInfo.getCurrentNode().getId();
    }
  }

  private void waitUntilNodeJoinsCluster() {
    synchronized (nodeJoinedEventListenerSync) {
      while (!clusterInfo.isNodeJoined()) {
        try {
          nodeJoinedEventListenerSync.wait(500);
        } catch (InterruptedException e) {
          // ignored
        }
      }
    }
  }

  public void nodeLeft(ClusterEvent evt) {
    debug("Received node left event: " + evt.getNode().getId());
    String nodeID = evt.getNode().getId();
    if (nodeID.equals(clusterInfo.getCurrentNode().getId())) {
      // ignore if ThisNodeLeft
      if (DEBUG) {
        debug("Ignoring nodeLeft from current node " + nodeID);
      }
    } else {
      if (this.removeIncoherentNode(nodeID)) {
        if (isClusterCoherent()) {
          clusteredStore.fireClusterCoherent(true);
        }
      }
    }
  }

  public void nodeJoined(ClusterEvent evt) {
    synchronized (nodeJoinedEventListenerSync) {
      nodeJoinedEventListenerSync.notifyAll();
    }
  }

  public void operationsDisabled(ClusterEvent evt) {
    // no-op
  }

  public void operationsEnabled(ClusterEvent evt) {
    // no-op
  }

  private void debug(String msg) {
    if (DEBUG) {
      LOGGER.info("[" + cacheName + "]: " + msg + " [ incoherentNodes.keySet(): " + incoherentNodes.keySet() + " ]");
    }
  }

}
