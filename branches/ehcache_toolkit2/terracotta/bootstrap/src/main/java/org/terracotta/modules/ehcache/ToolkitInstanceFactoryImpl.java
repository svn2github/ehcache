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
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.client.ToolkitClient;
import org.terracotta.toolkit.client.ToolkitClientBuilderFactory;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.ToolkitMapConfigFields;
import org.terracotta.toolkit.config.ToolkitMapConfigFields.PinningStore;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.client.TerracottaToolkitClientBuilderInternal;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;
import org.terracotta.toolkit.serializer.Serializer;

import java.io.Serializable;
import java.util.LinkedList;

public class ToolkitInstanceFactoryImpl implements ToolkitInstanceFactory {

  public static final String  DELIMITER                          = "|";

  private static final String EVENT_NOTIFIER_SUFFIX              = "event-notifier";
  private static final String EHCACHE_NAME_PREFIX                = "__tc_clustered-ehcache";
  private static final String CONFIG_NOTIFIER_SUFFIX             = "config-notifier";
  private static final String EHCACHE_TXNS_COMMIT_STATE_MAP_NAME = EHCACHE_NAME_PREFIX + DELIMITER + "txnsCommitState";
  private static final String EHCACHE_XA_TXNS_DECISION_MAP_NAME  = EHCACHE_NAME_PREFIX + DELIMITER + "xaTxnsDecision";
  private static final String ALL_SOFT_LOCKS_MAP_SUFFIX          = "softLocks";
  private static final String NEW_SOFT_LOCKS_LIST_SUFFIX         = "newSoftLocks";

  private static final String DEFAULT_ASYNC_LOCK                 = "__DEFAULT__ASYNC__LOCK__";
  private static final String ASYNC                              = "async";
  private static final String ASYNC_CONFIG_MAP                   = ASYNC + DELIMITER + "asyncConfigMap";
  private static final String CLUSTERED_STORE_CONFIG_MAP         = EHCACHE_NAME_PREFIX + DELIMITER + "configMap";

  protected final Toolkit     toolkit;

  private final ToolkitClient client;

  public ToolkitInstanceFactoryImpl(TerracottaClientConfiguration terracottaClientConfiguration) {
    this.client = createTerracottaClient(terracottaClientConfiguration);
    // TODO: support namespacing the toolkit
    this.toolkit = client.getToolkit();
  }

  private static ToolkitClient createTerracottaClient(TerracottaClientConfiguration terracottaClientConfiguration) {
    String config = null;
    if (!terracottaClientConfiguration.isUrlConfig()) {
      config = terracottaClientConfiguration.getEmbeddedConfig();
    } else {
      config = terracottaClientConfiguration.getUrl();
    }
    TerracottaToolkitClientBuilderInternal terracottaClientBuilder = (TerracottaToolkitClientBuilderInternal) ToolkitClientBuilderFactory
        .newTerracottaToolkitClientBuilder()
        .setDedicatedClient(terracottaClientConfiguration.isRejoin());
    if (terracottaClientConfiguration.isUrlConfig()) {
      terracottaClientBuilder.setTCConfigUrl(config);
    } else {
      terracottaClientBuilder.setTCConfigSnippet(config);
    }
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache");
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache.hibernate");
    return terracottaClientBuilder.buildToolkitClient();
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
    builder.maxCountLocalHeap((int)ehcacheConfig.getMaxEntriesLocalHeap());
    builder.maxBytesLocalHeap(ehcacheConfig.getMaxBytesLocalHeap());
    builder.maxBytesLocalOffheap(ehcacheConfig.getMaxBytesLocalOffHeap());
    builder.offheapEnabled(ehcacheConfig.isOverflowToOffHeap());
    builder.compressionEnabled(terracottaConfiguration.isCompressionEnabled());
    builder.copyOnReadEnabled(terracottaConfiguration.isCopyOnRead());

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
  public ToolkitMap<String, String> getOrCreateSearchAttributeTypesMap(Ehcache cache) {
    // implemented in ee version
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitMap<String, byte[]> getOrCreateSerializedExtractorsMap(Ehcache cache) {
    // implemented in ee version
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    client.shutdown();
  }

  @Override
  public ToolkitMap<String, SoftLockID> getOrCreateAllSoftLockMap(String cacheManagerName, String cacheName) {
    // TODO: what should be the local cache config for the map?
    Configuration config = toolkit.getConfigBuilderFactory().newToolkitMapConfigBuilder()
        .consistency(org.terracotta.toolkit.config.ToolkitMapConfigFields.Consistency.STRONG).build();
    return toolkit.getMap(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                          + ALL_SOFT_LOCKS_MAP_SUFFIX, config);
  }

  @Override
  public ToolkitList<SoftLockID> getOrCreateNewSoftLocksSet(String cacheManagerName, String cacheName) {
    return toolkit.getList(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                           + NEW_SOFT_LOCKS_LIST_SUFFIX);
  }

  @Override
  public ToolkitMap<String, AsyncConfig> getOrCreateAsyncConfigMap() {
    Configuration configuration = toolkit.getConfigBuilderFactory().newToolkitMapConfigBuilder()
        .consistency(org.terracotta.toolkit.config.ToolkitMapConfigFields.Consistency.STRONG).build();
    return toolkit.getMap(ASYNC_CONFIG_MAP, configuration);
  }

  @Override
  public ToolkitMap<String, LinkedList<String>> getOrCreateAsyncListNamesMap(String fullAsyncName) {
    Configuration configuration = toolkit.getConfigBuilderFactory().newToolkitMapConfigBuilder()
        .consistency(org.terracotta.toolkit.config.ToolkitMapConfigFields.Consistency.STRONG).build();
    return toolkit.getMap(fullAsyncName, configuration);
  }

  @Override
  public String getFullAsyncName(Ehcache cache, String asyncName) {
    String cacheMgrName = getCacheManagerName(cache);
    String cacheName = cache.getName();
    String fullAsyncName = cacheMgrName + DELIMITER + cacheName + DELIMITER + ASYNC + DELIMITER + asyncName;
    return fullAsyncName;
  }

  @Override
  public String getAsyncNode(String fullAsyncName, String nodeId) {
    return fullAsyncName + DELIMITER + nodeId;
  }

  @Override
  public ToolkitLock getAsyncWriteLock() {
    return toolkit.getLock(DEFAULT_ASYNC_LOCK, ToolkitLockType.WRITE);
  }

  @Override
  public Serializer getSerializer() {
    return ((ToolkitInternal) toolkit).getSerializer();
  }

  @Override
  public ToolkitMap<String, Serializable> getOrCreateClusteredStoreConfigMap(String cacheManagerName, String cacheName) {
    // TODO: what should be the local cache config for the map?
    Configuration config = toolkit.getConfigBuilderFactory().newToolkitMapConfigBuilder()
        .consistency(org.terracotta.toolkit.config.ToolkitMapConfigFields.Consistency.STRONG).build();
    return toolkit.getMap(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                          + CLUSTERED_STORE_CONFIG_MAP, config);
  }

  @Override
  public ToolkitMap<TransactionID, Decision> getOrCreateTransactionCommitStateMap(String cacheManagerName) {
    return null;
  }

  @Override
  public ToolkitReadWriteLock getSoftLockWriteLock(String cacheManagerName, String cacheName,
                                                   TransactionID transactionID) {
    return null;
  }

  @Override
  public ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName,
                                                    TransactionID transactionID) {
    return null;
  }

  @Override
  public ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName,
                                                      TransactionID transactionID) {
    return null;
  }
}
