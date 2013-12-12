package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.VersionedEntity;

/**
 * An abstract class for all cache entity objects providing all common properties, namely:
 *
 * <ul>
 *   <li>agentId</li>
 *   <li>name</li>
 *   <li>cacheManagerName</li>
 * </ul>
 *
 * @author brandony
 */
public abstract class AbstractCacheEntity extends VersionedEntity {
  private String agentId;

  private String name;

  private String cacheManagerName;

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCacheManagerName() {
    return cacheManagerName;
  }

  public void setCacheManagerName(String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
  }
}
