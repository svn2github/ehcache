/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.ToolkitMapConfigFields;
import org.terracotta.toolkit.config.ToolkitMapConfigFields.PinningStore;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;

import java.io.Serializable;

public class ToolkitInstanceFactoryImpl implements ToolkitInstanceFactory {

  private static final String EHCACHE_NAME_PREFIX    = "__tc_clustered-ehcache";
  private static final String DELIMITER              = "|";
  private static final String CONFIG_NOTIFIER_SUFFIX = "config-notifier";

  private final Toolkit       toolkit;

  public ToolkitInstanceFactoryImpl(Toolkit toolkit) {
    this.toolkit = toolkit;
  }

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

  public String getFullyQualifiedCacheName(Ehcache cache) {
    final String cacheMgrName;
    if (cache.getCacheManager().isNamed()) {
      cacheMgrName = cache.getCacheManager().getName();
    } else {
      cacheMgrName = TerracottaClusteredInstanceFactory.DEFAULT_CACHE_MANAGER_NAME;
    }
    return EHCACHE_NAME_PREFIX + DELIMITER + cacheMgrName + DELIMITER + cache.getName();
  }
}
