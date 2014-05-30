package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.VersionedEntity;

/**
 * @author brandony
 */
public class CacheManagerConfigEntity extends VersionedEntity {
  private String cacheManagerName;
  private String agentId;

  private String xml;

  public String getCacheManagerName() {
    return cacheManagerName;
  }

  public void setCacheManagerName(String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
  }

  public String getXml() {
    return xml;
  }

  public void setXml(String xml) {
    this.xml = xml;
  }

  @Override
  public String getAgentId() {
    return agentId;
  }

  @Override
  public void setAgentId(String agentId) {
    this.agentId =  agentId;
  }
}
