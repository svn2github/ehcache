/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource;

import org.terracotta.management.resource.AbstractEntityV2;

/**
 * @author brandony
 */
public class ElementEntityV2 extends AbstractEntityV2 {
  private String cacheName;

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }
}
