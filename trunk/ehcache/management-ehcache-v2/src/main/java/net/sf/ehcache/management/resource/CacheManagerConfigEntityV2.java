package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.AbstractEntityV2;

/**
 * @author brandony
 */
public class CacheManagerConfigEntityV2 extends AbstractEntityV2 {
  private String cacheManagerName;

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

}
