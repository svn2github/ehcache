package net.sf.ehcache.management.jmx;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class CacheManagerJmxClient {

  public static final String CACHE_MANAGER_JMX_NAME_ATTRIBUTE = "Name";

  public static Map<String, ObjectName> getAllCacheManagerObjectNamesByCacheManagerName(MBeanServer platformMBeanServer) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException {
    Set<ObjectName> allCacheManagerObjectNames = platformMBeanServer.queryNames(new ObjectName("net.sf.ehcache:type=SampledCacheManager,*"), null);

    Map<String, ObjectName> allCacheManagerObjectNamesByCacheManagerName = new HashMap<String, ObjectName>();
    for (ObjectName cacheManagerObjectName : allCacheManagerObjectNames) {
      String cacheManagerName = (String)platformMBeanServer.getAttribute(cacheManagerObjectName, CACHE_MANAGER_JMX_NAME_ATTRIBUTE);
      allCacheManagerObjectNamesByCacheManagerName.put(cacheManagerName, cacheManagerObjectName);
    }
    return allCacheManagerObjectNamesByCacheManagerName;
  }

}
