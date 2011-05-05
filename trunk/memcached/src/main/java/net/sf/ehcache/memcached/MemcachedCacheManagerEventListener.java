package net.sf.ehcache.memcached;

import com.thimbleware.jmemcached.MemCacheDaemon;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class MemcachedCacheManagerEventListener implements CacheManagerEventListener {

    private final static Logger LOG = LoggerFactory.getLogger(MemcachedCacheManagerEventListener.class);

    private volatile Status status = Status.STATUS_UNINITIALISED;
    private final CacheManager cacheManager;
    private final HashMap<String, Integer> cachePorts;
    private final ConcurrentMap<String, MemCacheDaemon> memCacheDaemons = new ConcurrentHashMap<String, MemCacheDaemon>();

    public MemcachedCacheManagerEventListener(CacheManager cacheManager, HashMap<String, Integer> cachePorts) {
        this.cacheManager = cacheManager;
        this.cachePorts = cachePorts;
    }

    public void init() throws CacheException {
        status = Status.STATUS_ALIVE;
    }

    public Status getStatus() {
        return status;
    }

    public void dispose() throws CacheException {
        for (String cacheName : cachePorts.keySet()) {
            notifyCacheRemoved(cacheName);
        }

        status = Status.STATUS_UNINITIALISED;
    }

    public void notifyCacheAdded(String cacheName) {
        Integer port = cachePorts.get(cacheName);
        if (port == null) {
            LOG.info("no memcached port configured for cache " + cacheName);
            return;
        }

        Ehcache cache = cacheManager.getEhcache(cacheName);

        MemCacheDaemon memCacheDaemon = new MemCacheDaemon();
        memCacheDaemon.setCache(new EhcacheCache(cache));
        memCacheDaemon.setAddr(new InetSocketAddress(port));
        memCacheDaemon.start();

        memCacheDaemons.put(cacheName, memCacheDaemon);
        LOG.info("cache " + cacheName + " listening on memcached port " + port);
    }

    public void notifyCacheRemoved(String cacheName) {
        MemCacheDaemon removedDaemon = memCacheDaemons.remove(cacheName);
        if (removedDaemon != null) {
            removedDaemon.stop();
        }
    }

}
