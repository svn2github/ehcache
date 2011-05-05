package net.sf.ehcache.memcached;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.CacheManagerEventListenerFactory;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class MemcachedCacheManagerEventListenerFactory extends CacheManagerEventListenerFactory {
    public static final String CACHE_PREFIX = "cache.";

    @Override
    public CacheManagerEventListener createCacheManagerEventListener(CacheManager cacheManager, Properties properties) {
        HashMap<String, Integer> cachePorts = new HashMap<String, Integer>();

        Set<String> propertyNames = properties.stringPropertyNames();
        for (String propertyName : propertyNames) {
            if (propertyName.startsWith(CACHE_PREFIX)) {
                String propertyValue = properties.getProperty(propertyName);

                String cacheName = propertyName.substring(CACHE_PREFIX.length());
                int port = Integer.parseInt(propertyValue);

                cachePorts.put(cacheName, port);
            }
        }

        return new MemcachedCacheManagerEventListener(cacheManager, cachePorts);
    }
}
