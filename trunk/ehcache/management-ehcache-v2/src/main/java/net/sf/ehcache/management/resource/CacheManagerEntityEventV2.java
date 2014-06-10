package net.sf.ehcache.management.resource;

import java.util.Collection;
import java.util.Map;

import org.terracotta.management.resource.VersionedEntityV2;

public class CacheManagerEntityEventV2 extends VersionedEntityV2 {

  private  String cacheManagerName;
  private  String type;
  private  Collection<Map<String, Object>> cacheEntities;


  public CacheManagerEntityEventV2() {
    super();
  }

  public Collection<Map<String, Object>> getCacheEntities() {
    return cacheEntities;
  }

  public void setCacheEntities(Collection<Map<String, Object>> cacheEntities) {
    this.cacheEntities = cacheEntities;
  }

  public void setCacheManagerName(String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
  }

  public void setType(String type) {
    this.type = type;
  }

  public CacheManagerEntityEventV2(String cacheManagerName, String type, Collection<Map<String, Object>> entities) {
    super();
    this.cacheManagerName = cacheManagerName;
    this.type = type;
    this.cacheEntities = entities;

  }

  @Override
  public String getAgentId() {
    // TODO Auto-generated method stub
    return "";
  }

  @Override
  public void setAgentId(String agentId) {
    // TODO Auto-generated method stub

  }

  public String getCacheManagerName() {
    return cacheManagerName;
  }


  public String getType() {
    return type;
  }

}
