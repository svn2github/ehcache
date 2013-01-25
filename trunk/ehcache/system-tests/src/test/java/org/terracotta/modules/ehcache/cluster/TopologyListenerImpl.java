/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;

import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class TopologyListenerImpl implements ClusterTopologyListener {

  private static Logger       LOG            = Logger.getLogger(TopologyListenerImpl.class.getName());

  private final AtomicInteger nodesJoined    = new AtomicInteger();
  private final AtomicInteger nodesLeft      = new AtomicInteger();
  private final AtomicInteger clusterOffline = new AtomicInteger();
  private final AtomicInteger clusterOnline  = new AtomicInteger();
  private String              clusterId      = "UNINITIALIZED";

  @Override
  public void clusterOffline(ClusterNode arg0) {
    LOG.info(arg0.getId() + " ...Cluster Offline.");
    clusterOffline.incrementAndGet();
  }

  @Override
  public void clusterOnline(ClusterNode arg0) {
    LOG.info(this.clusterId + " received Cluster Online for " + arg0.getId());
    clusterOnline.incrementAndGet();
  }

  @Override
  public void nodeJoined(ClusterNode arg0) {
    LOG.info(this.clusterId + " received Node Joined for " + arg0.getId());
    nodesJoined.incrementAndGet();

  }

  @Override
  public void nodeLeft(ClusterNode arg0) {
    LOG.info(arg0.getId() + " ...Node Left");
    nodesLeft.incrementAndGet();

  }

  @Override
  public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
    LOG.info(oldNode.getId() + " Rejoined as " + newNode.getId());
  }

  public int getNodesJoined() {
    return nodesJoined.get();
  }

  public int getNodesLeft() {
    return nodesLeft.get();
  }

  public int getClusterOffline() {
    return clusterOffline.get();
  }

  public int getClusterOnline() {
    return clusterOnline.get();
  }

  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }

}
