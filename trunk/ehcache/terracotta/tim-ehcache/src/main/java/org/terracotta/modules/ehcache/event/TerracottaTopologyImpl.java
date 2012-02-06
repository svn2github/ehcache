/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.cluster.ClusterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A bridge from a DsoClusterTopology in dso-space to a TerracottaTopology in Ehcache-space.
 */
public class TerracottaTopologyImpl implements CacheCluster {

  private final ClusterInfo                                   cluster;

  private final CopyOnWriteArrayList<ClusterTopologyListener> listeners = new CopyOnWriteArrayList<ClusterTopologyListener>();
  private final ReentrantReadWriteLock.WriteLock              writeLock = new ReentrantReadWriteLock().writeLock();

  public TerracottaTopologyImpl(ClusterInfo cluster) {
    this.cluster = cluster;
  }

  public ClusterScheme getScheme() {
    return ClusterScheme.TERRACOTTA;
  }

  public ClusterNode getCurrentNode() {
    return new TerracottaNodeImpl(cluster.getCurrentNode());
  }

  public ClusterNode waitUntilNodeJoinsCluster() {
    return new TerracottaNodeImpl(cluster.waitUntilNodeJoinsCluster());
  }

  public Collection<ClusterNode> getNodes() {
    Collection<org.terracotta.cluster.ClusterNode> dsoNodes = cluster.getClusterTopology().getNodes();
    Collection<ClusterNode> nodes = new ArrayList<ClusterNode>();
    for (org.terracotta.cluster.ClusterNode node : dsoNodes) {
      nodes.add(new TerracottaNodeImpl(node));
    }
    return nodes;
  }

  public boolean addTopologyListener(ClusterTopologyListener listener) {
    boolean rv;
    writeLock.lock();
    try {
      rv = listeners.add(listener);
      if (rv) {
        addInternal(listener);
      }
    } finally {
      writeLock.unlock();
    }
    return rv;
  }

  public boolean removeTopologyListener(ClusterTopologyListener listener) {
    boolean rv;
    writeLock.lock();
    try {
      rv = listeners.remove(listener);
      if (rv) {
        removeInternal(listener);
      }
    } finally {
      writeLock.unlock();
    }
    return rv;
  }

  private void addInternal(ClusterTopologyListener listener) {
    cluster.addClusterListener(new ClusterListenerAdapter(listener));
  }

  private void removeInternal(ClusterTopologyListener listener) {
    cluster.removeClusterListener(new ClusterListenerAdapter(listener));
  }

  public boolean isClusterOnline() {
    return cluster.areOperationsEnabled();
  }

  public List<ClusterTopologyListener> getTopologyListeners() {
    writeLock.lock();
    try {
      return listeners;
    } finally {
      writeLock.unlock();
    }
  }
}
