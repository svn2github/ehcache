/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheInitializationHelper;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.constructs.classloader.InternalClassLoaderAwareCache;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.terracotta.InternalEhcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.collections.servermap.ServerMapLocalStoreConfig;
import org.terracotta.modules.ehcache.LocalVMResources;
import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.adapters.ServerMapLocalStoreFactoryAdapter;

public class ServerMapLocalStoreFactoryImpl extends ServerMapLocalStoreFactoryAdapter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerMapLocalStoreFactoryImpl.class);

  public ServerMapLocalStoreFactoryImpl() {
    // default public constructor
  }

  private InternalEhcache getOrCreateEhcacheLocalCache(CacheManager cacheManager,
                                                       CacheInitializationHelper cacheInitializer,
                                                       CacheConfiguration localStoreCacheConfig) {
    InternalEhcache localStoreCache = null;
    synchronized (cacheManager) {
      localStoreCache = LocalVMResources.getInstance()
          .getRegisteredCache(cacheManager, localStoreCacheConfig.getName());
      if (localStoreCache == null) {
        localStoreCache = new Cache(localStoreCacheConfig);
        if (localStoreCacheConfig.isOverflowToOffHeap()) {
          ClassLoader l1Loader = TerracottaClusteredInstanceFactory.class.getClassLoader();
          localStoreCache = new InternalClassLoaderAwareCache(localStoreCache, l1Loader);
        }
        cacheInitializer.initializeEhcache(localStoreCache);
        localStoreCache.getCacheEventNotificationService()
            .registerListener(new CacheDisposalEventListener(localStoreCache));
        LocalVMResources.getInstance().registerCache(localStoreCache);
      }
    }
    return localStoreCache;
  }

  @Override
  public <K, V> ServerMapLocalStore<K, V> getOrCreateServerMapLocalStore(ServerMapLocalStoreConfig config) {
    CacheManager cacheManager = LocalVMResources.getInstance()
        .getRegisteredCacheManager(config.getLocalStoreManagerName());
    CacheInitializationHelper cacheInitializer = new CacheInitializationHelper(cacheManager);
    if (cacheManager == null) { throw new AssertionError("CacheManager is null, localStoreCacheManagerName: "
                                                         + config.getLocalStoreManagerName()); }

    InternalEhcache localStoreCache = getOrCreateEhcacheLocalCache(cacheManager, cacheInitializer,
                                                                   getLocalStoreCacheConfig(cacheManager, config));

    ServerMapLocalStore<Object, Object> serverMapLocalStore = new EhcacheSMLocalStore(localStoreCache);
    return (ServerMapLocalStore<K, V>) serverMapLocalStore;
  }

  private Ehcache getAppLayerClusteredEhcache(CacheManager cacheManager, ServerMapLocalStoreConfig config) {
    final String name = config.getLocalStoreName();
    Ehcache appCache = cacheManager.getEhcache(name);
    if (appCache == null) {
      appCache = LocalVMResources.getInstance().getRegisteredCache(cacheManager, name);
    }
    if (appCache == null) {
      LOGGER.warn("Cache name '" + name + "' not found in local vm resources either");
      String msg = "Clustered Cache with name '" + name + "' not found in local CacheManager (looked up using id: '"
                   + config.getLocalStoreManagerName() + "'). Are you sure you have the same ehcache.xml in all nodes?";
      LOGGER.error(msg);
      throw new AssertionError(msg);
    }
    return appCache;
  }

  private CacheConfiguration getLocalStoreCacheConfig(CacheManager cacheManager, ServerMapLocalStoreConfig config) {
    Ehcache appCache = getAppLayerClusteredEhcache(cacheManager, config);
    CacheConfiguration appCacheConfig = appCache.getCacheConfiguration();
    CacheConfiguration localStoreConfig = new CacheConfiguration(getLocalCacheName(appCache), 0);

    // never use the disk store
    localStoreConfig.setOverflowToDisk(false);

    // wire up config
    if (appCacheConfig.getMaxEntriesLocalHeap() > 0) {
      localStoreConfig.setMaxEntriesLocalHeap(appCacheConfig.getMaxEntriesLocalHeap() * 2 + 1);
    }

    if (appCacheConfig.getMaxBytesLocalHeap() > 0) {
      localStoreConfig.setMaxBytesLocalHeap(appCacheConfig.getMaxBytesLocalHeap());
    }

    localStoreConfig.setOverflowToOffHeap(appCacheConfig.isOverflowToOffHeap());
    if (appCacheConfig.isOverflowToOffHeap()) {
      long maxBytesLocalOffHeap = appCacheConfig.getMaxBytesLocalOffHeap();
      if (maxBytesLocalOffHeap > 0) {
        localStoreConfig.setMaxBytesLocalOffHeap(maxBytesLocalOffHeap);
      }
    }

    PinningConfiguration pinningConfig = appCacheConfig.getPinningConfiguration();
    if (pinningConfig != null) {
      localStoreConfig.addPinning(new PinningConfiguration().store(pinningConfig.getStore()));
    }

    // always use clock eviction policy
    localStoreConfig.setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy.CLOCK);

    // TODO: any other config?
    return localStoreConfig;
  }

  private String getLocalCacheName(Ehcache cache) {
    return "local-shadow-cache-for-" + cache.getName() + "-uuid-" + cache.getGuid();
  }

  private static class CacheDisposalEventListener extends CacheEventListenerAdapter {
    private final Ehcache cache;

    private CacheDisposalEventListener(final Ehcache cache) {
      this.cache = cache;
    }

    @Override
    public void dispose() {
      LocalVMResources.getInstance().unregisterCache(cache);
    }
  }
}
