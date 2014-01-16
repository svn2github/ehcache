package net.sf.ehcache.management.jmx;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class CacheJmxClient {

  public static final String CACHE_JMX_NAME_ATTRIBUTE = "CacheName";

  public static Map<String, Map<String, ObjectName>> getAllCacheObjectNamesByCacheNameByCacheManagerName(MBeanServer platformMBeanServer, List<String> cacheManagerNames) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException {
    Set<ObjectName> allCacheObjectNames = platformMBeanServer.queryNames(new ObjectName("net.sf.ehcache:type=SampledCache,*"), null);

    Map<String, Map<String, ObjectName>> allCacheObjectNamesByCacheName = new HashMap<String, Map<String, ObjectName>>();
    for (ObjectName cacheObjectName : allCacheObjectNames) {
      String cacheName = (String)platformMBeanServer.getAttribute(cacheObjectName, CACHE_JMX_NAME_ATTRIBUTE);

      String cacheManagerName = cacheObjectName.getKeyProperty("SampledCacheManager");
      if (cacheManagerNames == null || cacheManagerNames.contains(cacheManagerName)) {
        Map<String, ObjectName> map = allCacheObjectNamesByCacheName.get(cacheManagerName);
        if (map == null) {
          map = new HashMap<String, ObjectName>();
          allCacheObjectNamesByCacheName.put(cacheManagerName, map);
        }
        map.put(cacheName, cacheObjectName);
      }
    }
    return allCacheObjectNamesByCacheName;
  }

}
