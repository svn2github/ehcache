package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.AbstractEntityV2;

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
public abstract class AbstractCacheEntityV2 extends AbstractEntityV2 {

  private String name;

  private String cacheManagerName;


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
