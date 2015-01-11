/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.AbstractEntityV2;

/**
 * <p>
 * A {@link VersionedEntity} representing a cache configuration resource from the management API.
 * </p>
 * 
 * @author brandony
 * 
 */
public class CacheConfigEntityV2 extends AbstractEntityV2 {
  private String cacheName;
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

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }
}
