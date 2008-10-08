package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.loader.CacheLoaderFactory;
import net.sf.ehcache.Ehcache;
import net.sf.jsr107cache.CacheLoader;

import java.util.Map;
import java.util.Properties;

/**
 * A factory to create JMSCacheLoaders.
 *
 *
 * @author Greg Luck
 *
 */
public class JMSCacheLoaderFactory extends CacheLoaderFactory {


    /**
     * Creates a CacheLoader using the JSR107 creational mechanism.
     * This method is called from {@link net.sf.ehcache.jcache.JCacheFactory}
     *
     * @param environment the same environment passed into {@link net.sf.ehcache.jcache.JCacheFactory}.
     *                    This factory can extract any properties it needs from the environment.
     * @return a constructed CacheLoader
     */
    public CacheLoader createCacheLoader(Map environment) {
        return null;
    }

    /**
     * Creates a CacheLoader using the Ehcache configuration mechanism at the time the associated cache
     * is created.
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @return a constructed CacheLoader
     */
    public net.sf.ehcache.loader.CacheLoader createCacheLoader(Properties properties) {
        return null;
    }

    /**
     * Creates a CacheLoader using the Ehcache configuration mechanism at the time the associated cache
     * is created.
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @param cache the cache this loader is bound to
     * @return a constructed CacheLoader
     */
    public net.sf.ehcache.loader.CacheLoader createCacheLoader(Ehcache cache, Properties properties) {
        return null;
    }
}
