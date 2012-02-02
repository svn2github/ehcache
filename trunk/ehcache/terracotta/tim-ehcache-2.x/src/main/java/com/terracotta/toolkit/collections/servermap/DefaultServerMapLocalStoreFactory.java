/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheInitializationHelper;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.terracotta.InternalEhcache;

import org.terracotta.collections.servermap.ServerMapLocalStoreConfig;
import org.terracotta.modules.ehcache.store.servermap.EhcacheSMLocalStore;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;

public class DefaultServerMapLocalStoreFactory implements ServerMapLocalStoreFactory {

  public DefaultServerMapLocalStoreFactory() {
    // default public constructor
  }

  public <K, V> ServerMapLocalStore<K, V> getOrCreateServerMapLocalStore(ServerMapLocalStoreConfig config) {
    InternalEhcache localStoreCache = getOrCreateEhcacheLocalCache(config);

    return (ServerMapLocalStore<K, V>) new EhcacheSMLocalStore(localStoreCache);
  }

  private InternalEhcache getOrCreateEhcacheLocalCache(ServerMapLocalStoreConfig config) {
    InternalEhcache ehcache;
    CacheManager cacheManager = getOrCreateCacheManager(config);
    synchronized (cacheManager) {
      final String localCacheName = "local-cache-for-" + config.getLocalStoreName();
      ehcache = (InternalEhcache) cacheManager.getEhcache(config.getLocalStoreName());
      if (ehcache == null) {
        ehcache = createCache(localCacheName, config);
        new CacheInitializationHelper(cacheManager).initializeEhcache(ehcache);
      }
    }
    return ehcache;
  }

  private CacheManager getOrCreateCacheManager(ServerMapLocalStoreConfig config) {
    CacheManager cacheManager = CacheManager.getCacheManager(config.getLocalStoreManagerName());
    if (cacheManager == null) {
      cacheManager = CacheManager.create(new Configuration().name(config.getLocalStoreManagerName()));
    }
    return cacheManager;
  }

  private Cache createCache(String cacheName, ServerMapLocalStoreConfig config) {
    CacheConfiguration cacheConfig = new CacheConfiguration(cacheName, config.getMaxCountLocalHeap())
        .overflowToDisk(false).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.CLOCK);

    // wire up config
    if (config.getMaxCountLocalHeap() > 0) {
      cacheConfig.setMaxEntriesLocalHeap(config.getMaxCountLocalHeap() * 2 + 1);
    }

    if (config.getMaxBytesLocalHeap() > 0) {
      cacheConfig.setMaxBytesLocalHeap(config.getMaxBytesLocalHeap());
    }

    cacheConfig.setOverflowToOffHeap(config.isOverflowToOffheap());
    if (config.isOverflowToOffheap()) {
      long maxBytesLocalOffHeap = config.getMaxBytesLocalOffheap();
      if (maxBytesLocalOffHeap > 0) {
        cacheConfig.setMaxBytesLocalOffHeap(maxBytesLocalOffHeap);
      }
    }

    return new Cache(cacheConfig);
  }
}
