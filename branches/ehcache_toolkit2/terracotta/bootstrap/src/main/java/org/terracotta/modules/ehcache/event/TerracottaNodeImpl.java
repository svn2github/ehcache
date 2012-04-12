/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import org.terracotta.toolkit.cluster.ClusterNode;

import java.net.UnknownHostException;

/**
 * A bridge from ClusterNode in Toolkit to TerracottaNode in Ehcache-space.
 */
public class TerracottaNodeImpl implements net.sf.ehcache.cluster.ClusterNode {

  private final ClusterNode node;

  public TerracottaNodeImpl(ClusterNode node) {
    this.node = node;
  }

  public String getHostname() {
    try {
      return this.node.getAddress().getHostName();
    } catch (UnknownHostException e) {
      return null;
    }
  }

  public String getId() {
    return this.node.getId();
  }

  public String getIp() {
    try {
      return this.node.getAddress().getHostAddress();
    } catch (UnknownHostException e) {
      return null;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((node == null) ? 0 : node.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TerracottaNodeImpl other = (TerracottaNodeImpl) obj;
    if (node == null) {
      if (other.node != null) return false;
    } else if (!node.equals(other.node)) return false;
    return true;
  }

}
