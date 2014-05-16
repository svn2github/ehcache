package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.VersionedEntityV2;

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
public abstract class AbstractCacheEntityV2 extends VersionedEntityV2 {
  private String agentId;

  private String name;

  private String cacheManagerName;

  @Override
  public String getAgentId() {
    return agentId;
  }

  @Override
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
