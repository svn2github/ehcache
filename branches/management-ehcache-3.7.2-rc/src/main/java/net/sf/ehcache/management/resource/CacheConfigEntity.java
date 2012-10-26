/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package net.sf.ehcache.management.resource;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * <p>
 * A {@link VersionedEntity} representing a cache configuration resource from the management API.
 * </p>
 * 
 * @author brandony
 * 
 */
public class CacheConfigEntity extends CacheManagerConfigEntity {
  private String cacheName;

  @XmlAttribute
  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }
}
