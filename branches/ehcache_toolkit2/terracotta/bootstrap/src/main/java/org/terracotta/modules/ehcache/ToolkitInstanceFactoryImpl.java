/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.client.TerracottaClientStaticFactory;
import org.terracotta.toolkit.client.ToolkitClient;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.ToolkitMapConfigFields;
import org.terracotta.toolkit.config.ToolkitMapConfigFields.PinningStore;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;

import java.io.Serializable;

public class ToolkitInstanceFactoryImpl implements ToolkitInstanceFactory {

  protected static final String            DELIMITER                  = "|";

  private static final String              FULLY_QUALIFIED_NAME_MAP   = "fullyQualifiedNameMap";
  private static final String              EVENT_NOTIFIER_SUFFIX      = "event-notifier";
  private static final String              EHCACHE_NAME_PREFIX        = "__tc_clustered-ehcache";
  private static final String              CONFIG_NOTIFIER_SUFFIX     = "config-notifier";
  private static final String              EHCACHE_CLUSTERED_STORE_ID = EHCACHE_NAME_PREFIX + DELIMITER
                                                                        + "clusteredStoreId";

  protected final Toolkit                  toolkit;

  private final ToolkitAtomicLong          clusteredStoreId;
  private final ToolkitMap<String, String> fullyQualifiedNames;
  private final ToolkitReadWriteLock       lock;

  public ToolkitInstanceFactoryImpl(TerracottaClientConfiguration terracottaClientConfiguration) {
    ToolkitClient client = createTerracottaClient(terracottaClientConfiguration);
    // TODO: support namespacing the toolkit
    this.toolkit = client.getToolkit();
    clusteredStoreId = toolkit.getAtomicLong(EHCACHE_CLUSTERED_STORE_ID);
    fullyQualifiedNames = toolkit.getMap(EHCACHE_NAME_PREFIX + DELIMITER + FULLY_QUALIFIED_NAME_MAP);
    lock = toolkit.getReadWriteLock(EHCACHE_NAME_PREFIX + DELIMITER + "factoryLock");
  }

  private static ToolkitClient createTerracottaClient(TerracottaClientConfiguration terracottaClientConfiguration) {
    if (!terracottaClientConfiguration.isUrlConfig()) {
      // TODO: is this to be supported?
      throw new IllegalArgumentException("Embedded tc-config no longer supported");
    }
    if (terracottaClientConfiguration.isRejoin()) {
      return TerracottaClientStaticFactory.getFactory().createDedicatedClient(terracottaClientConfiguration.getUrl());
    } else {
      return TerracottaClientStaticFactory.getFactory().getOrCreateClient(terracottaClientConfiguration.getUrl());
    }
  }

  @Override
  public Toolkit getToolkit() {
    return toolkit;
  }

  @Override
  public ToolkitCacheWithMetadata<Object, Serializable> getOrCreateToolkitCache(Ehcache cache) {
    final Configuration clusteredCacheConfig = createClusteredMapConfig(toolkit.getConfigBuilderFactory()
        .newToolkitCacheConfigBuilder(), cache);
    return (ToolkitCacheWithMetadata) toolkit.getCache(getFullyQualifiedCacheName(cache), clusteredCacheConfig);
  }

  @Override
  public ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(Ehcache cache) {
    return toolkit.getNotifier(getFullyQualifiedCacheName(cache) + DELIMITER + CONFIG_NOTIFIER_SUFFIX);
  }

  @Override
  public ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(Ehcache cache) {
    return toolkit.getNotifier(getFullyQualifiedCacheName(cache) + DELIMITER + EVENT_NOTIFIER_SUFFIX);
  }

  private static Configuration createClusteredMapConfig(ToolkitCacheConfigBuilder builder, Ehcache cache) {
    final CacheConfiguration ehcacheConfig = cache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    builder.maxTTISeconds((int) ehcacheConfig.getTimeToIdleSeconds());
    builder.maxTTLSeconds((int) ehcacheConfig.getTimeToLiveSeconds());
    builder.maxTotalCount(ehcacheConfig.getMaxElementsOnDisk());
    builder.localCacheEnabled(terracottaConfiguration.isLocalCacheEnabled());

    if (terracottaConfiguration.isSynchronousWrites()) {
      builder.consistency(org.terracotta.toolkit.config.ToolkitMapConfigFields.Consistency.SYNCHRONOUS_STRONG);
    } else if (terracottaConfiguration.getConsistency() == Consistency.EVENTUAL) {
      builder.consistency(org.terracotta.toolkit.config.ToolkitMapConfigFields.Consistency.EVENTUAL);
    } else {
      builder.consistency(org.terracotta.toolkit.config.ToolkitMapConfigFields.Consistency.STRONG);
    }

    if (terracottaConfiguration.getConcurrency() == TerracottaConfiguration.DEFAULT_CONCURRENCY) {
      builder.concurrency(calculateCorrectConcurrency(ehcacheConfig));
    } else {
      builder.concurrency(terracottaConfiguration.getConcurrency());
    }

    final String cmName = cache.getCacheManager().isNamed() ? cache.getCacheManager().getName()
        : TerracottaClusteredInstanceFactory.DEFAULT_CACHE_MANAGER_NAME;
    builder.localCacheEnabled(terracottaConfiguration.isLocalCacheEnabled());
    builder.localStoreManagerName(cmName);
    if (ehcacheConfig.getPinningConfiguration() != null) {
      builder.pinningStore(getPinningStoreForConfiguration(ehcacheConfig));
    }
    builder.maxCountLocalHeap(ehcacheConfig.getMaxEntriesLocalHeap());
    builder.maxBytesLocalHeap(ehcacheConfig.getMaxBytesLocalHeap());
    builder.maxBytesLocalOffheap(ehcacheConfig.getMaxBytesLocalOffHeap());
    builder.offheapEnabled(ehcacheConfig.isOverflowToOffHeap());

    return builder.build();
  }

  private static int calculateCorrectConcurrency(CacheConfiguration cacheConfiguration) {
    int maxElementOnDisk = cacheConfiguration.getMaxElementsOnDisk();
    if (maxElementOnDisk <= 0 || maxElementOnDisk >= ToolkitMapConfigFields.DEFAULT_CONCURRENCY) { return ToolkitMapConfigFields.DEFAULT_CONCURRENCY; }
    int concurrency = 1;
    while (concurrency * 2 <= maxElementOnDisk) {// this while loop is not very time consuming, maximum it will do 8
                                                 // iterations
      concurrency *= 2;
    }
    return concurrency;
  }

  private static PinningStore getPinningStoreForConfiguration(CacheConfiguration ehcacheConfig) {
    switch (ehcacheConfig.getPinningConfiguration().getStore()) {
      case INCACHE:
        return PinningStore.INCACHE;
      case LOCALHEAP:
        return PinningStore.LOCALHEAP;
      case LOCALMEMORY:
        return PinningStore.LOCALMEMORY;
    }
    // don't do this as the "default" in the switch block so the compiler can catch errors
    throw new AssertionError("unknown Pinning Configuration: " + ehcacheConfig.getPinningConfiguration().getStore());
  }

  @Override
  public String getFullyQualifiedCacheName(Ehcache cache) {
    String fullyQualifiedNameWithoutId = getFullyQualifiedNameWithoutId(cache);
    // try with unsafeGet
    String cacheFQN = fullyQualifiedNames.unsafeGet(fullyQualifiedNameWithoutId);
    if (cacheFQN != null) { return cacheFQN; }

    // try with read locks
    lock.readLock().lock();
    try {
      cacheFQN = fullyQualifiedNames.get(fullyQualifiedNameWithoutId);
      if (cacheFQN != null) { return cacheFQN; }
    } finally {
      lock.readLock().unlock();
    }

    // finally try with write locks
    lock.writeLock().lock();
    try {
      cacheFQN = fullyQualifiedNames.get(fullyQualifiedNameWithoutId);
      if (cacheFQN != null) { return cacheFQN; }

      cacheFQN = fullyQualifiedNameWithoutId + clusteredStoreId.incrementAndGet();
      fullyQualifiedNames.put(fullyQualifiedNameWithoutId, cacheFQN);
      return cacheFQN;
    } finally {
      lock.writeLock().unlock();
    }

  }

  private String getFullyQualifiedNameWithoutId(Ehcache cache) {
    final String cacheMgrName;
    if (cache.getCacheManager().isNamed()) {
      cacheMgrName = cache.getCacheManager().getName();
    } else {
      cacheMgrName = TerracottaClusteredInstanceFactory.DEFAULT_CACHE_MANAGER_NAME;
    }
    return EHCACHE_NAME_PREFIX + DELIMITER + cacheMgrName + DELIMITER + cache.getName();
  }

  @Override
  public ToolkitReadWriteLock getOrCreateStoreLock(Ehcache cache) {
    return toolkit.getReadWriteLock(getFullyQualifiedCacheName(cache) + DELIMITER + "storeRWLock");
  }

  @Override
  public ToolkitMap<String, String> getOrCreateSearchAttributeTypesMap(Ehcache cache) {
    // implemented in ee version
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitMap<String, byte[]> getOrCreateSerializedExtractorsMap(Ehcache cache) {
    // implemented in ee version
    throw new UnsupportedOperationException();
  }

}
