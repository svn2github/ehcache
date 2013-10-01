/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.terracotta.toolkit.cluster.ClusterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TerracottaTopologyImpl implements CacheCluster {

  private final ClusterInfo                                   cluster;
  private final CopyOnWriteArrayList<ClusterTopologyListener> listeners = new CopyOnWriteArrayList<ClusterTopologyListener>();
  private final ReentrantReadWriteLock.WriteLock              writeLock = new ReentrantReadWriteLock().writeLock();

  public TerracottaTopologyImpl(ClusterInfo clusterInfo) {
    this.cluster = clusterInfo;
  }

  @Override
  public ClusterScheme getScheme() {
    return ClusterScheme.TERRACOTTA;
  }

  @Override
  public ClusterNode getCurrentNode() {
    return new TerracottaNodeImpl(cluster.getCurrentNode());
  }

  @Override
  public ClusterNode waitUntilNodeJoinsCluster() {
    return new TerracottaNodeImpl(cluster.getCurrentNode());
  }

  @Override
  public Collection<ClusterNode> getNodes() {
    Collection<org.terracotta.toolkit.cluster.ClusterNode> toolkitNodes = cluster.getNodes();
    Collection<ClusterNode> nodes = new ArrayList<ClusterNode>();
    for (org.terracotta.toolkit.cluster.ClusterNode node : toolkitNodes) {
      nodes.add(new TerracottaNodeImpl(node));
    }
    return nodes;
  }

  @Override
  public boolean isClusterOnline() {
    return cluster.areOperationsEnabled();
  }

  @Override
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

  @Override
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
    cluster.addClusterListener(new ClusterListenerAdapter(listener, cluster));
  }

  private void removeInternal(ClusterTopologyListener listener) {
    cluster.removeClusterListener(new ClusterListenerAdapter(listener, cluster));
  }

  @Override
  public List<ClusterTopologyListener> getTopologyListeners() {
    writeLock.lock();
    try {
      return Collections.unmodifiableList(listeners);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public void removeAllListeners() {
    writeLock.lock();
    try {
      for (ClusterTopologyListener listener : listeners) {
        removeInternal(listener);
      }
      listeners.clear();
    } finally {
      writeLock.unlock();
    }
  }

}
