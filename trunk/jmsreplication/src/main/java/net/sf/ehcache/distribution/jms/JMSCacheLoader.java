package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.loader.CacheLoader;
import net.sf.jsr107cache.CacheException;

import java.util.Map;
import java.util.Collection;

/**
 * @author Greg Luck
 *
 */
public class JMSCacheLoader implements CacheLoader {

    private JMSCachePeer jmsCachePeer;


    /**
     * 
     */
    public JMSCacheLoader(JMSCachePeer jmsCachePeer) {

        this.jmsCachePeer = jmsCachePeer;

    }


    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will call through to the load(key) method, rather than this method, where the argument is null.
     *
     * @param key      the key to load the object for
     * @param argument can be anything that makes sense to the loader
     * @return the Object loaded
     * @throws net.sf.jsr107cache.CacheException
     *
     */
    public Object load(Object key, Object argument) throws CacheException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will use the loadAll(key) method where the argument is null.
     *
     * @param keys     the keys to load objects for
     * @param argument can be anything that makes sense to the loader
     * @return a map of Objects keyed by the collection of keys passed in.
     * @throws net.sf.jsr107cache.CacheException
     *
     */
    public Map loadAll(Collection keys, Object argument) throws CacheException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Gets the name of a CacheLoader
     *
     * @return the name of this CacheLoader
     */
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * loads an object. Application writers should implement this
     * method to customize the loading of cache object. This method is called
     * by the caching service when the requested object is not in the cache.
     * <p/>
     *
     * @param key the key identifying the object being loaded
     * @return The object that is to be stored in the cache.
     * @throws net.sf.jsr107cache.CacheException
     *
     */
    public Object load(Object key) throws CacheException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * loads multiple object. Application writers should implement this
     * method to customize the loading of cache object. This method is called
     * by the caching service when the requested object is not in the cache.
     * <p/>
     *
     * @param keys a Collection of keys identifying the objects to be loaded
     * @return A Map of objects that are to be stored in the cache.
     * @throws net.sf.jsr107cache.CacheException
     *
     */
    public Map loadAll(Collection keys) throws CacheException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
