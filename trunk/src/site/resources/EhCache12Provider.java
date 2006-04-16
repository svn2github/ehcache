package org.hibernate.cache.entry;

import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.EhCacheProvider;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EhCache;
import org.hibernate.cache.Timestamper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.ehcache.CacheManager;

import java.util.Properties;

/**
 * Cache Provider plugin for Hibernate
 * <p/>
 * Use <code>hibernate.cache.provider_class=org.hibernate.cache.EhCache12Provider</code>
 * in Hibernate 3.x or later
 * <p/>
 * Updated for ehcache-1.2. Note this provider requires ehcache-1.2.jar. Make sure ehcache-1.1.jar or earlier
 * is not in the classpath or it will not work.
 * <p/>
 * See http://ehcache.sf.net for documentation on ehcache
 * <p/>
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
public class EhCache12Provider implements CacheProvider {

    private static final Log log = LogFactory.getLog(EhCache12Provider.class);


    private CacheManager manager;

    /**
     * The Hibernate system property specifying the location of the ehcache configuration file name.
     * <p/
     * If not set, ehcache.xml will be looked for in the root of the classpath.
     * <p/>
     * If set to say ehcache-1.xml, ehcache-1.xml will be looked for in the root of the classpath.
     */
    public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

    /**
     * Builds a Cache.
     * <p>
     * Even though this method provides properties, they are not used.
     * Properties for EHCache are specified in the ehcache.xml file.
     * Configuration will be read from ehcache.xml for a cache declaration
     * where the name attribute matches the name parameter in this builder.
     *
     * @param name the name of the cache. Must match a cache configured in ehcache.xml
     * @param properties not used
     * @return a newly built cache will be built and initialised
     * @throws org.hibernate.cache.CacheException inter alia, if a cache of the same name already exists
     */
    public Cache buildCache(String name, Properties properties) throws CacheException {
        try {
            net.sf.ehcache.Cache cache = manager.getCache(name);
            if (cache == null) {
                EhCache12Provider.log.warn("Could not find configuration [" + name + "]; using defaults.");
                manager.addCache(name);
                cache = manager.getCache(name);
                EhCache12Provider.log.debug("started EHCache region: " + name);
            }
            return new EhCache(cache);
        }
        catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Returns the next timestamp.
     */
    public long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * Callback to perform any necessary initialization of the underlying cache implementation
     * during SessionFactory construction.
     * <p/>
     * Specify
     *
     * @param properties current configuration settings.
     */
    public void start(Properties properties) throws CacheException {
        try {
            String configurationResourceName = (String) properties.get(NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME);
            if (configurationResourceName == null || configurationResourceName.length() == 0) {
                manager = new CacheManager();
            } else {
                manager = new CacheManager(configurationResourceName);
            }
        }
        catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Callback to perform any necessary cleanup of the underlying cache implementation
     * during SessionFactory.close().
     */
    public void stop() {
        if ( manager != null ) {
            manager.shutdown();
            manager = null;
        }
    }

    public boolean isMinimalPutsEnabledByDefault() {
        return false;
    }

}
