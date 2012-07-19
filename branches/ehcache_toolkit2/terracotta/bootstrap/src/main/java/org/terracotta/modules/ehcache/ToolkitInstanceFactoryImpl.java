/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.collections.SerializationHelper;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;
import org.terracotta.modules.ehcache.txn.ClusteredSoftLockIDKey;
import org.terracotta.modules.ehcache.txn.SerializedReadCommittedClusteredSoftLock;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitStore;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.ToolkitCacheConfigFields.PinningStore;
import org.terracotta.toolkit.config.ToolkitStoreConfigFields;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;

import java.io.Serializable;
import java.util.Set;

public class ToolkitInstanceFactoryImpl implements ToolkitInstanceFactory {

  public static final String  DELIMITER                                = "|";

  private static final String EVENT_NOTIFIER_SUFFIX                    = "event-notifier";
  private static final String EHCACHE_NAME_PREFIX                      = "__tc_clustered-ehcache";
  private static final String CONFIG_NOTIFIER_SUFFIX                   = "config-notifier";
  private static final String EHCACHE_TXNS_DECISION_STATE_MAP_NAME     = EHCACHE_NAME_PREFIX + DELIMITER
                                                                         + "txnsDecision";
  private static final String ALL_SOFT_LOCKS_MAP_SUFFIX                = "softLocks";
  private static final String NEW_SOFT_LOCKS_LIST_SUFFIX               = "newSoftLocks";

  private static final String DEFAULT_ASYNC_LOCK                       = "__DEFAULT__ASYNC__LOCK__";
  private static final String ASYNC                                    = "async";
  private static final String ASYNC_CONFIG_MAP                         = ASYNC + DELIMITER + "asyncConfigMap";
  private static final String CLUSTERED_STORE_CONFIG_MAP               = EHCACHE_NAME_PREFIX + DELIMITER + "configMap";

  private static final String EHCACHE_TXNS_SOFTLOCK_WRITE_LOCK_NAME    = EHCACHE_NAME_PREFIX + DELIMITER
                                                                         + "softWriteLock";

  private static final String EHCACHE_TXNS_SOFTLOCK_FREEZE_LOCK_NAME   = EHCACHE_NAME_PREFIX + DELIMITER
                                                                         + "softFreezeLock";

  private static final String EHCACHE_TXNS_SOFTLOCK_NOTIFIER_LOCK_NAME = EHCACHE_NAME_PREFIX + DELIMITER
                                                                         + "softNotifierLock";

  protected final Toolkit     toolkit;

  public ToolkitInstanceFactoryImpl(TerracottaClientConfiguration terracottaClientConfiguration) {
    this.toolkit = createTerracottaToolkit(terracottaClientConfiguration);
  }

  private static Toolkit createTerracottaToolkit(TerracottaClientConfiguration terracottaClientConfiguration) {
    String config = null;
    if (!terracottaClientConfiguration.isUrlConfig()) {
      config = terracottaClientConfiguration.getEmbeddedConfig();
    } else {
      config = terracottaClientConfiguration.getUrl();
    }
    TerracottaToolkitBuilder terracottaClientBuilder = new TerracottaToolkitBuilder();
    if (terracottaClientConfiguration.isUrlConfig()) {
      terracottaClientBuilder.setTCConfigUrl(config);
    } else {
      terracottaClientBuilder.setTCConfigSnippet(config);
    }
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache");
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache.hibernate");
    return terracottaClientBuilder.buildToolkit();
  }

  @Override
  public Toolkit getToolkit() {
    return toolkit;
  }

  @Override
  public ToolkitCacheWithMetadata<String, Serializable> getOrCreateToolkitCache(Ehcache cache) {
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
      builder.consistency(org.terracotta.toolkit.config.ToolkitStoreConfigFields.Consistency.SYNCHRONOUS_STRONG);
    } else if (terracottaConfiguration.getConsistency() == Consistency.EVENTUAL) {
      builder.consistency(org.terracotta.toolkit.config.ToolkitStoreConfigFields.Consistency.EVENTUAL);
    } else {
      builder.consistency(org.terracotta.toolkit.config.ToolkitStoreConfigFields.Consistency.STRONG);
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
    builder.maxCountLocalHeap((int) ehcacheConfig.getMaxEntriesLocalHeap());
    builder.maxBytesLocalHeap(ehcacheConfig.getMaxBytesLocalHeap());
    builder.maxBytesLocalOffheap(ehcacheConfig.getMaxBytesLocalOffHeap());
    builder.offheapEnabled(ehcacheConfig.isOverflowToOffHeap());
    builder.compressionEnabled(terracottaConfiguration.isCompressionEnabled());
    builder.copyOnReadEnabled(terracottaConfiguration.isCopyOnRead());

    return builder.build();
  }

  private static int calculateCorrectConcurrency(CacheConfiguration cacheConfiguration) {
    int maxElementOnDisk = cacheConfiguration.getMaxElementsOnDisk();
    if (maxElementOnDisk <= 0 || maxElementOnDisk >= ToolkitStoreConfigFields.DEFAULT_CONCURRENCY) { return ToolkitStoreConfigFields.DEFAULT_CONCURRENCY; }
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
    return getFullyQualifiedCacheName(getCacheManagerName(cache), cache.getName());
  }

  public static String getFullyQualifiedCacheName(String cacheMgrName, String cacheName) {
    return EHCACHE_NAME_PREFIX + DELIMITER + cacheMgrName + DELIMITER + cacheName;
  }

  private static String getCacheManagerName(Ehcache cache) {
    final String cacheMgrName;
    if (cache.getCacheManager().isNamed()) {
      cacheMgrName = cache.getCacheManager().getName();
    } else {
      cacheMgrName = TerracottaClusteredInstanceFactory.DEFAULT_CACHE_MANAGER_NAME;
    }
    return cacheMgrName;
  }

  @Override
  public ToolkitReadWriteLock getOrCreateStoreLock(Ehcache cache) {
    return toolkit.getReadWriteLock(getFullyQualifiedCacheName(cache) + DELIMITER + "storeRWLock");
  }

  @Override
  public ToolkitCache<String, String> getOrCreateSearchAttributeTypesMap(Ehcache cache) {
    // implemented in ee version
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitCache<String, byte[]> getOrCreateSerializedExtractorsMap(Ehcache cache) {
    // implemented in ee version
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    toolkit.shutdown();
  }

  @Override
  public SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock> getOrCreateAllSoftLockMap(String cacheManagerName,
                                                                                                      String cacheName) {
    // TODO: what should be the local cache config for the map?
    Configuration config = toolkit.getConfigBuilderFactory().newToolkitStoreConfigBuilder()
        .consistency(org.terracotta.toolkit.config.ToolkitStoreConfigFields.Consistency.STRONG).build();
    ToolkitCache<String, SerializedReadCommittedClusteredSoftLock> map = toolkit
        .getCache(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER + ALL_SOFT_LOCKS_MAP_SUFFIX,
                  config);
    return new SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock>(map);

  }

  @Override
  public ToolkitList<SerializedReadCommittedClusteredSoftLock> getOrCreateNewSoftLocksSet(String cacheManagerName,
                                                                                String cacheName) {
    return toolkit.getList(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                           + NEW_SOFT_LOCKS_LIST_SUFFIX);
  }

  @Override
  public ToolkitCache<String, AsyncConfig> getOrCreateAsyncConfigMap() {
    return toolkit.getCache(ASYNC_CONFIG_MAP);
  }

  @Override
  public ToolkitStore<String, Set<String>> getOrCreateAsyncListNamesMap(String fullAsyncName) {
    Configuration configuration = toolkit.getConfigBuilderFactory().newToolkitStoreConfigBuilder()
        .consistency(org.terracotta.toolkit.config.ToolkitStoreConfigFields.Consistency.STRONG).build();
    return toolkit.getStore(fullAsyncName, configuration);
  }

  @Override
  public String getFullAsyncName(Ehcache cache) {
    String cacheMgrName = getCacheManagerName(cache);
    String cacheName = cache.getName();
    String fullAsyncName = cacheMgrName + DELIMITER + cacheName + DELIMITER + ASYNC;
    return fullAsyncName;
  }

  @Override
  public ToolkitLock getAsyncWriteLock() {
    return toolkit.getLock(DEFAULT_ASYNC_LOCK, ToolkitLockType.WRITE);
  }

  @Override
  public ToolkitCache<String, Serializable> getOrCreateClusteredStoreConfigMap(String cacheManagerName, String cacheName) {
    // TODO: what should be the local cache config for the map?
    return toolkit.getCache(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                          + CLUSTERED_STORE_CONFIG_MAP);
  }

  @Override
  public SerializedToolkitCache<TransactionID, Decision> getOrCreateTransactionCommitStateMap(String cacheManagerName) {
    // TODO: what should be the local cache config for the map?
    Configuration config = toolkit.getConfigBuilderFactory().newToolkitStoreConfigBuilder()
        .consistency(org.terracotta.toolkit.config.ToolkitStoreConfigFields.Consistency.STRONG).build();
    ToolkitCache<String, Decision> map = toolkit.getCache(cacheManagerName + DELIMITER
                                                      + EHCACHE_TXNS_DECISION_STATE_MAP_NAME, config);
    return new SerializedToolkitCache<TransactionID, Decision>(map);
  }

  @Override
  public ToolkitLock getSoftLockWriteLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                          Object key) {

    return toolkit.getLock(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                           + serializeToString(transactionID) + DELIMITER + serializeToString(key) + DELIMITER
                           + EHCACHE_TXNS_SOFTLOCK_WRITE_LOCK_NAME, ToolkitLockType.WRITE);
  }

  private static String serializeToString(Object serializable) {
    try {
      return SerializationHelper.serializeToString(serializable);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName,
                                                    TransactionID transactionID, Object key) {

    return toolkit.getReadWriteLock(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                                    + serializeToString(transactionID) + DELIMITER + serializeToString(key) + DELIMITER
                                    + EHCACHE_TXNS_SOFTLOCK_FREEZE_LOCK_NAME);
  }

  @Override
  public ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName,
                                                      TransactionID transactionID, Object key) {

    return toolkit.getReadWriteLock(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                                    + serializeToString(transactionID) + DELIMITER + serializeToString(key) + DELIMITER
                                    + EHCACHE_TXNS_SOFTLOCK_NOTIFIER_LOCK_NAME);
  }
}
