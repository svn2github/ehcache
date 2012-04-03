/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ColorCache {
  private static final CacheManager  cacheManager  = new CacheManager();
  private static final ColorDatabase colorDatabase = new ColorDatabase();

  public ColorCache() {
    /**/
  }

  public Color getColor(String name) {
    Element elem = getCache().get(name);
    if (elem == null) {
      Color color = colorDatabase.getColor(name);
      if (color == null) { return null; }
      getCache().put(elem = new Element(name, color));
    }
    return (Color) elem.getValue();
  }

  private Color getCachedColor(String name) {
    Element elem = getCache().get(name);
    return elem != null ? (Color) elem.getValue() : null;
  }

  public String[] getColorNames() {
    @SuppressWarnings("unchecked")
    Iterator<String> keys = ((List<String>) getCache().getKeys()).iterator();
    List<String> list = new ArrayList<String>();
    while (keys.hasNext()) {
      String name = keys.next();
      if (getCachedColor(name) != null) {
        list.add(name);
      }
    }
    return list.toArray(new String[list.size()]);
  }

  public long getTTL() {
    return getCache().getCacheConfiguration().getTimeToLiveSeconds();
  }

  public long getTTI() {
    return getCache().getCacheConfiguration().getTimeToIdleSeconds();
  }

  public int getSize() {
    return getCache().getSize();
  }

  private Ehcache getCache() {
    return cacheManager.getEhcache("colors");
  }
}
