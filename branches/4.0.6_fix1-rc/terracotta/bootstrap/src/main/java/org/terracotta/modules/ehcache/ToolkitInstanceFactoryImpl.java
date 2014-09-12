/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.collections.SerializationHelper;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.modules.ehcache.event.CacheDisposalNotification;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;
import org.terracotta.modules.ehcache.store.ToolkitNonStopConfiguration;
import org.terracotta.modules.ehcache.store.nonstop.ToolkitNonstopDisableConfig;
import org.terracotta.modules.ehcache.transaction.ClusteredSoftLockIDKey;
import org.terracotta.modules.ehcache.transaction.SerializedReadCommittedClusteredSoftLock;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public class ToolkitInstanceFactoryImpl implements ToolkitInstanceFactory {

  public static final String  DELIMITER                                = "|";

  private static final String EVENT_NOTIFIER_SUFFIX                    = "event-notifier";
  private static final String DISPOSAL_NOTIFIER_SUFFIX                 = "disposal-notifier";
  private static final String EHCACHE_NAME_PREFIX                      = "__tc_clustered-ehcache";
  private static final String CONFIG_NOTIFIER_SUFFIX                   = "config-notifier";
  private static final String EHCACHE_TXNS_DECISION_STATE_MAP_NAME     = EHCACHE_NAME_PREFIX + DELIMITER
                                                                         + "txnsDecision";
  private static final String ALL_SOFT_LOCKS_MAP_SUFFIX                = "softLocks";
  private static final String NEW_SOFT_LOCKS_LIST_SUFFIX               = "newSoftLocks";

  private static final String ASYNC_CONFIG_MAP                         = "asyncConfigMap";
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
    updateDefaultNonStopConfig(toolkit);
  }

  // Constructor to enable unit testing
  ToolkitInstanceFactoryImpl(Toolkit toolkit) {
    this.toolkit = toolkit;
  }

  private void updateDefaultNonStopConfig(Toolkit toolkitParam) {
    ToolkitNonstopDisableConfig disableNonStop = new ToolkitNonstopDisableConfig();
    NonStopConfigurationRegistry nonStopConfigurationRegistry = toolkitParam.getFeature(ToolkitFeatureType.NONSTOP)
        .getNonStopConfigurationRegistry();
    for (ToolkitObjectType t : ToolkitObjectType.values()) {
      try {
        nonStopConfigurationRegistry.registerForType(disableNonStop, t);
      } catch (UnsupportedOperationException e) {
        // expected for Barrier and BlockingQueue.
        if (!(t == ToolkitObjectType.BARRIER || t == ToolkitObjectType.BLOCKING_QUEUE)) { throw e; }
      }
    }
  }

  private static Toolkit createTerracottaToolkit(TerracottaClientConfiguration terracottaClientConfiguration) {
    TerracottaToolkitBuilder terracottaClientBuilder = new TerracottaToolkitBuilder();
    EhcacheTcConfig ehcacheTcConfig = EhcacheTcConfig.create(terracottaClientConfiguration);
    switch (ehcacheTcConfig.type) {
      case URL:
        terracottaClientBuilder.setTCConfigUrl(ehcacheTcConfig.tcConfigUrlOrSnippet);
        break;
      case EMBEDDED_TC_CONFIG:
      case FILE:
        terracottaClientBuilder.setTCConfigSnippet(ehcacheTcConfig.tcConfigUrlOrSnippet);
        break;
    }
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache");
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache.hibernate");
    terracottaClientBuilder.setRejoinEnabled(terracottaClientConfiguration.isRejoin());
    return terracottaClientBuilder.buildToolkit();
  }

  @Override
  public Toolkit getToolkit() {
    return toolkit;
  }

  @Override
  public ToolkitCacheInternal<String, Serializable> getOrCreateToolkitCache(Ehcache cache) {
    final Configuration clusteredCacheConfig = createClusteredCacheConfig(cache);
    addNonStopConfigForCache(cache);
    return getOrCreateToolkitCache(cache.getCacheManager().getName(), cache.getName(), clusteredCacheConfig);
  }

  private ToolkitCacheInternal<String, Serializable> getOrCreateToolkitCache(String cacheManagerMan, String cacheName, Configuration config) {
    return (ToolkitCacheInternal<String, Serializable>) toolkit.getCache(getFullyQualifiedCacheName(cacheManagerMan, cacheName), config, Serializable.class);
  }

  @Override
  public ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(Ehcache cache) {
    return getOrCreateConfigChangeNotifier(cache.getCacheManager().getName(), cache.getName());
  }

  private ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(String cacheManagerName, String cacheName) {
    return toolkit.getNotifier(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER + CONFIG_NOTIFIER_SUFFIX,
      CacheConfigChangeNotificationMsg.class);
  }
  @Override
  public ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(Ehcache cache) {
    return getOrCreateCacheEventNotifier(cache.getCacheManager().getName(), cache.getName());
  }

  @Override
  public ToolkitNotifier<CacheDisposalNotification> getOrCreateCacheDisposalNotifier(Ehcache cache) {
    return toolkit.getNotifier(getFullyQualifiedCacheName(cache.getCacheManager().getName(), cache.getName())
                               + DELIMITER + DISPOSAL_NOTIFIER_SUFFIX, CacheDisposalNotification.class);
  }

  private ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(String cacheManagerName, String cacheName) {
    return toolkit.getNotifier(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER + EVENT_NOTIFIER_SUFFIX,
        CacheEventNotificationMsg.class);
  }

  private static Configuration createClusteredCacheConfig(Ehcache cache) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    final CacheConfiguration ehcacheConfig = cache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    builder.maxTTISeconds((int) ehcacheConfig.getTimeToIdleSeconds());
    builder.maxTTLSeconds((int) ehcacheConfig.getTimeToLiveSeconds());
    builder.localCacheEnabled(terracottaConfiguration.isLocalCacheEnabled());

    // Fix for Dev-9223. Dont set anything incase of Default value. Assuming tookit and ehcache defaults are aligned.
    if (ehcacheConfig.getMaxEntriesInCache() != CacheConfiguration.DEFAULT_MAX_ENTRIES_IN_CACHE) {
      if (ehcacheConfig.getMaxEntriesInCache() > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Values greater than Integer.MAX_VALUE are not currently supported.");
      } else {
        builder.maxTotalCount((int) ehcacheConfig.getMaxEntriesInCache());
      }
    }

    if (terracottaConfiguration.isSynchronousWrites()) {
      builder.consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.SYNCHRONOUS_STRONG);
    } else if (terracottaConfiguration.getConsistency() == Consistency.EVENTUAL) {
      builder.consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.EVENTUAL);
    } else {
      builder.consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.STRONG);
    }

    if (terracottaConfiguration.getConcurrency() == TerracottaConfiguration.DEFAULT_CONCURRENCY) {
      builder.concurrency(calculateCorrectConcurrency(ehcacheConfig));
    } else {
      builder.concurrency(terracottaConfiguration.getConcurrency());
    }

    final String cmName = cache.getCacheManager().isNamed() ? cache.getCacheManager().getName()
        : TerracottaClusteredInstanceFactory.DEFAULT_CACHE_MANAGER_NAME;
    builder.localCacheEnabled(terracottaConfiguration.isLocalCacheEnabled());
    builder.configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, cmName);
    builder.pinnedInLocalMemory(isPinnedInLocalMemory(ehcacheConfig));
    builder.evictionEnabled(!isPinnedInCache(ehcacheConfig));
    builder.maxCountLocalHeap((int) ehcacheConfig.getMaxEntriesLocalHeap());
    builder.maxBytesLocalHeap(ehcacheConfig.getMaxBytesLocalHeap());
    builder.maxBytesLocalOffheap(ehcacheConfig.getMaxBytesLocalOffHeap());
    builder.offheapEnabled(ehcacheConfig.isOverflowToOffHeap());
    builder.compressionEnabled(terracottaConfiguration.isCompressionEnabled());
    builder.copyOnReadEnabled(ehcacheConfig.isCopyOnRead());

    return builder.build();
  }

    private static boolean isPinnedInCache(final CacheConfiguration ehcacheConfig) {
    return ehcacheConfig.getPinningConfiguration() != null
           && ehcacheConfig.getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;
  }

  private static int calculateCorrectConcurrency(CacheConfiguration cacheConfiguration) {
    int maxElementOnDisk = cacheConfiguration.getMaxElementsOnDisk();
    if (maxElementOnDisk <= 0 || maxElementOnDisk >= ToolkitConfigFields.DEFAULT_CONCURRENCY) { return ToolkitConfigFields.DEFAULT_CONCURRENCY; }
    int concurrency = 1;
    while (concurrency * 2 <= maxElementOnDisk) {// this while loop is not very time consuming, maximum it will do 8
                                                 // iterations
      concurrency *= 2;
    }
    return concurrency;
  }

  private static boolean isPinnedInLocalMemory(CacheConfiguration ehcacheConfig) {
    return ehcacheConfig.getPinningConfiguration() != null
           && ehcacheConfig.getPinningConfiguration().getStore() == PinningConfiguration.Store.LOCALMEMORY;
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
  public ToolkitLock getOrCreateStoreLock(Ehcache cache) {
    return toolkit.getLock(getFullyQualifiedCacheName(cache) + DELIMITER + "storeRWLock");
  }

  @Override
  public ToolkitMap<String, AttributeExtractor> getOrCreateExtractorsMap(Ehcache cache) {
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
    Configuration config = new ToolkitStoreConfigBuilder()
        .consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.STRONG).build();
    ToolkitCache<String, SerializedReadCommittedClusteredSoftLock> map = toolkit
        .getCache(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER + ALL_SOFT_LOCKS_MAP_SUFFIX,
                  config, SerializedReadCommittedClusteredSoftLock.class);
    return new SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock>(map);

  }

  @Override
  public ToolkitMap<SerializedReadCommittedClusteredSoftLock, Integer> getOrCreateNewSoftLocksSet(String cacheManagerName,
                                                                                                  String cacheName) {
    return toolkit.getMap(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                          + NEW_SOFT_LOCKS_LIST_SUFFIX, SerializedReadCommittedClusteredSoftLock.class, Integer.class);
  }

  @Override
  public ToolkitMap<String, AsyncConfig> getOrCreateAsyncConfigMap() {
    return toolkit.getMap(ASYNC_CONFIG_MAP, String.class, AsyncConfig.class);
  }

  @Override
  public ToolkitMap<String, Set<String>> getOrCreateAsyncListNamesMap(String fullAsyncName) {
    ToolkitMap asyncListNames = toolkit.getMap(fullAsyncName, String.class, Set.class);
    return asyncListNames;
  }

  @Override
  public ToolkitMap<String, Serializable> getOrCreateClusteredStoreConfigMap(String cacheManagerName, String cacheName) {
    // TODO: what should be the local cache config for the map?
    return toolkit.getMap(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                          + CLUSTERED_STORE_CONFIG_MAP, String.class, Serializable.class);
  }

  @Override
  public SerializedToolkitCache<TransactionID, Decision> getOrCreateTransactionCommitStateMap(String cacheManagerName) {
    // TODO: what should be the local cache config for the map?
    Configuration config = new ToolkitStoreConfigBuilder()
        .consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.SYNCHRONOUS_STRONG).build();
    ToolkitCache<String, Decision> map = toolkit.getCache(cacheManagerName + DELIMITER
                                                              + EHCACHE_TXNS_DECISION_STATE_MAP_NAME, config,
                                                          Decision.class);
    return new SerializedToolkitCache<TransactionID, Decision>(map);
  }

  @Override
  public ToolkitLock getSoftLockWriteLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                          Object key) {

    return toolkit.getLock(getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                           + serializeToString(transactionID) + DELIMITER + serializeToString(key) + DELIMITER
                           + EHCACHE_TXNS_SOFTLOCK_WRITE_LOCK_NAME);
  }

  @Override
  public ToolkitLock getLockForCache(Ehcache cache, String lockName) {
    return toolkit.getLock(getFullyQualifiedCacheName(cache) + DELIMITER + lockName);
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

  @Override
  public boolean destroy(final String cacheManagerName, final String cacheName) {
    getOrCreateAllSoftLockMap(cacheManagerName, cacheName).destroy();
    getOrCreateNewSoftLocksSet(cacheManagerName, cacheName).destroy();
    getOrCreateCacheEventNotifier(cacheManagerName, cacheName).destroy();
    getOrCreateConfigChangeNotifier(cacheManagerName, cacheName).destroy();
    getOrCreateToolkitCache(cacheManagerName, cacheName,
        new ToolkitCacheConfigBuilder().maxCountLocalHeap(1)
            .maxBytesLocalOffheap(0)
            .build()).destroy();

    // We always write the transactional mode into this config map, so theoretically if the cache existed, this map
    // won't be empty.
    ToolkitMap clusteredStoreConfigMap = getOrCreateClusteredStoreConfigMap(cacheManagerName, cacheName);
    boolean existed = !clusteredStoreConfigMap.isEmpty();
    clusteredStoreConfigMap.destroy();

    return existed;
  }

  private void addNonStopConfigForCache(Ehcache cache) {
    final CacheConfiguration ehcacheConfig = cache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    ToolkitNonStopConfiguration nonstopConfiguration = new ToolkitNonStopConfiguration(
                                                                                       terracottaConfiguration
                                                                                           .getNonstopConfiguration());
    toolkit.getFeature(ToolkitFeatureType.NONSTOP).getNonStopConfigurationRegistry()
        .registerForInstance(nonstopConfiguration, getFullyQualifiedCacheName(cache), ToolkitObjectType.CACHE);

  }

  @Override
  public void removeNonStopConfigforCache(Ehcache cache) {
    toolkit.getFeature(ToolkitFeatureType.NONSTOP).getNonStopConfigurationRegistry()
        .deregisterForInstance(getFullyQualifiedCacheName(cache), ToolkitObjectType.CACHE);

  }

  private static class EhcacheTcConfig {
    private enum Type {
      URL, EMBEDDED_TC_CONFIG, FILE
    }

    private final Type   type;
    private final String tcConfigUrlOrSnippet;

    private EhcacheTcConfig(Type type, String config) {
      this.type = type;
      this.tcConfigUrlOrSnippet = config;
    }

    public static EhcacheTcConfig create(TerracottaClientConfiguration config) {
      if (config.isUrlConfig()) {
        String urlOrFilePath = config.getUrl();
        if (isFile(urlOrFilePath)) {
          return new EhcacheTcConfig(Type.FILE, slurpFile(urlOrFilePath));
        } else if (isValidURL(urlOrFilePath)) {
          return new EhcacheTcConfig(Type.EMBEDDED_TC_CONFIG, fetchConfigFromURL(urlOrFilePath));
        } else {
          return new EhcacheTcConfig(Type.URL, urlOrFilePath);
        }
      } else {
        return new EhcacheTcConfig(Type.EMBEDDED_TC_CONFIG, config.getEmbeddedConfig());
      }
    }

    private static String slurpFile(String urlOrFilePath) {
      try {
        return fetchConfigFromStream(new FileInputStream(urlOrFilePath));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    private static boolean isFile(String urlOrFilePath) {
      File file = new File(urlOrFilePath);
      return file.exists() && file.isFile();
    }
    
    private static String fetchConfigFromURL(String urlOrFilePath) {
      try {
        return fetchConfigFromStream(new URL(urlOrFilePath).openStream());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
    private static String fetchConfigFromStream(InputStream inputStream) {
      try {
        StringBuilder builder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = br.readLine()) != null) {
          builder.append(line);
        }
        return builder.toString();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private static boolean isValidURL(String urlOrFilePath) {
      try {
        new URL(urlOrFilePath);
        return true;
      } catch (MalformedURLException e) {
        return false;
      }
    }

  }
}
