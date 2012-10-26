/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.VersionedEntity;

/**
 * @author brandony
 */
public class ElementEntity extends VersionedEntity {
  private String agentId;

  private String cacheName;

  @Override
  public String getAgentId() {
    return agentId;
  }

  @Override
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }
}
