/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public abstract class EhcacheStatsUtils {
  public final static String EHCACHE_DOMAIN                         = "net.sf.ehcache";
  public final static String SAMPLED_CACHE_MANAGER_BEAN_NAME_PREFIX = EHCACHE_DOMAIN + ":type=SampledCacheManager";
  public final static String SAMPLED_CACHE_BEAN_NAME_PREFIX         = EHCACHE_DOMAIN + ":type=SampledCache";

  public static String mbeanSafe(String s) {
    return s == null ? "" : s.replaceAll(":|=|\n", ".");
  }

  public static ObjectName getSampledCacheManagerBeanName(String name) throws MalformedObjectNameException {
    if (name == null) { throw new IllegalArgumentException("name"); }
    return new ObjectName(SAMPLED_CACHE_MANAGER_BEAN_NAME_PREFIX + ",name=" + mbeanSafe(name));
  }

  public static ObjectName getSampledCacheBeanName(String cacheManagerName, String cacheName)
      throws MalformedObjectNameException {
    if (cacheManagerName == null) { throw new IllegalArgumentException("cacheManagerName"); }
    if (cacheName == null) { throw new IllegalArgumentException("cacheName"); }
    return new ObjectName(SAMPLED_CACHE_BEAN_NAME_PREFIX + ",SampledCacheManager=" + mbeanSafe(cacheManagerName)
                          + ",name=" + mbeanSafe(cacheName));
  }

  public static ObjectName replaceKey(ObjectName beanName, String key, String value) {
    Hashtable keyPropertyList = beanName.getKeyPropertyList();
    Hashtable propertyList = new Hashtable(keyPropertyList);
    propertyList.put(key, value);

    try {
      return new ObjectName(beanName.getDomain(), propertyList);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ObjectName renameKey(ObjectName beanName, String oldKey, String newKey) {
    Hashtable keyPropertyList = beanName.getKeyPropertyList();
    Hashtable propertyList = new Hashtable(keyPropertyList);
    Object value = propertyList.remove(oldKey);

    if (value != null) {
      propertyList.put(newKey, value);
    }

    try {
      return new ObjectName(beanName.getDomain(), propertyList);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
