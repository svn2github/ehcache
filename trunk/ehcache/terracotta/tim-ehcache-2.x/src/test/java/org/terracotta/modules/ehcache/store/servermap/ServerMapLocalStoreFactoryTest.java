/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.collections.servermap.ServerMapLocalStoreConfig;
import org.terracotta.collections.servermap.ServerMapLocalStoreConfigParameters;
import org.terracotta.modules.ehcache.LocalVMResources;
import org.terracotta.modules.ehcache.LocalVMResourcesTestUtil;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;

import java.util.concurrent.ConcurrentMap;

import junit.framework.Assert;

public class ServerMapLocalStoreFactoryTest {

  private static final String   CACHE_MANAGER_NAME                = "cacheManagerName";
  private static final String   CACHE_MAX_ENTRIES_LOCAL_HEAP_NAME = "maxEntriesLocalHeapCacheName";
  private static final String   CACHE_MAX_BYTES_LOCAL_HEAP_NAME   = "maxBytesLocalHeapCacheName";
  private volatile CacheManager cm;

  @Before
  public void setup() {
    Configuration config = new Configuration().name(CACHE_MANAGER_NAME);
    cm = CacheManager.create(config);

    CacheConfiguration cacheConfig = new CacheConfiguration().name(CACHE_MAX_ENTRIES_LOCAL_HEAP_NAME)
        .maxEntriesLocalHeap(10).timeToIdleSeconds(123).timeToLiveSeconds(456);
    cm.addCache(new Cache(cacheConfig));

    cacheConfig = new CacheConfiguration().name(CACHE_MAX_BYTES_LOCAL_HEAP_NAME)
        .maxBytesLocalHeap(11, MemoryUnit.MEGABYTES).timeToIdleSeconds(0).timeToLiveSeconds(456789);
    cm.addCache(new Cache(cacheConfig));

    LocalVMResources.getInstance().registerCacheManager(CACHE_MANAGER_NAME, cm);
  }

  @After
  public void teardown() {
    cm.shutdown();
    try {
      // cacheManagers are deregistered automatically on shutdown
      LocalVMResources.getInstance().getRegisteredCacheManager(CACHE_MANAGER_NAME);
      Assert.fail("Should have thrown exception");
    } catch (CacheException e) {
      Assert
          .assertEquals(
                        "Expected a cacheManager to be registered with uuid: cacheManagerName, but was mapped to (className: null): null",
                        e.getMessage());
    }

    ConcurrentMap<String, Object> registeredResources = LocalVMResourcesTestUtil.getRegisteredResources();
    System.out.println("After shutdown: " + registeredResources);
    // local shadow caches are not disposed as not added to cacheManager's list of caches
    Assert.assertEquals(2, registeredResources.size());

    // shutdown explicitly
    for (Object o : registeredResources.values()) {
      Ehcache ehcache = (Ehcache) o;
      ehcache.dispose();
    }

    registeredResources = LocalVMResourcesTestUtil.getRegisteredResources();
    System.out.println("After shutdown and dispose: " + registeredResources);
    Assert.assertEquals(0, registeredResources.size());

  }

  private Ehcache getShadowEhcache(String cacheName) {
    ServerMapLocalStoreFactory factory = new ServerMapLocalStoreFactoryImpl();
    ServerMapLocalStoreConfigParameters configParams = new ServerMapLocalStoreConfigParameters()
        .localStoreManagerName(CACHE_MANAGER_NAME).localStoreName(cacheName);
    ServerMapLocalStoreConfig smConfig = new ServerMapLocalStoreConfig(configParams);

    ConcurrentMap<String, Object> registeredResources = LocalVMResourcesTestUtil.getRegisteredResources();
    System.out.println("Before creation: " + registeredResources);
    int beforeSize = registeredResources.size();

    EhcacheSMLocalStore ehcacheStore = (EhcacheSMLocalStore) factory
        .<Object, Object> getOrCreateServerMapLocalStore(smConfig);

    ConcurrentMap<String, Object> registeredResourcesAfterCreation = LocalVMResourcesTestUtil.getRegisteredResources();
    System.out.println("After creation: " + registeredResourcesAfterCreation);

    Assert.assertEquals(beforeSize, registeredResourcesAfterCreation.size() - 1);

    return ehcacheStore.getLocalEhcache();
  }

  @Test
  public void testShadowCacheConfig() {
    Ehcache shadowEhcache = getShadowEhcache(CACHE_MAX_ENTRIES_LOCAL_HEAP_NAME);
    Assert.assertFalse(shadowEhcache.getCacheConfiguration().isOverflowToDisk());
    Assert.assertFalse(shadowEhcache.getCacheConfiguration().isOverflowToOffHeap());
    Assert.assertEquals(10 * 2 + 1, shadowEhcache.getCacheConfiguration().getMaxEntriesLocalHeap());
    Assert.assertEquals(0, shadowEhcache.getCacheConfiguration().getMaxBytesLocalHeap());
    Assert.assertEquals(0, shadowEhcache.getCacheConfiguration().getMaxBytesLocalOffHeap());
    Assert.assertEquals(MemoryStoreEvictionPolicy.CLOCK, shadowEhcache.getCacheConfiguration()
        .getMemoryStoreEvictionPolicy());

    shadowEhcache = getShadowEhcache(CACHE_MAX_BYTES_LOCAL_HEAP_NAME);
    Assert.assertFalse(shadowEhcache.getCacheConfiguration().isOverflowToDisk());
    Assert.assertFalse(shadowEhcache.getCacheConfiguration().isOverflowToOffHeap());
    Assert.assertEquals(0, shadowEhcache.getCacheConfiguration().getMaxEntriesLocalHeap());
    Assert.assertEquals(MemoryUnit.MEGABYTES.toBytes(11), shadowEhcache.getCacheConfiguration().getMaxBytesLocalHeap());
    Assert.assertEquals(0, shadowEhcache.getCacheConfiguration().getMaxBytesLocalOffHeap());
    Assert.assertEquals(MemoryStoreEvictionPolicy.CLOCK, shadowEhcache.getCacheConfiguration()
        .getMemoryStoreEvictionPolicy());

    // one cacheManager + its CacheManagerDisposalListener, 2 local store ehcaches
    Assert.assertEquals(4, LocalVMResourcesTestUtil.getRegisteredResources().size());

  }

}
