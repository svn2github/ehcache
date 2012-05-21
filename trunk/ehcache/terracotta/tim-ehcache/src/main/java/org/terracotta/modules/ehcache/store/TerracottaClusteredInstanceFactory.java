/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.util.ProductInfo;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.api.Terracotta;
import org.terracotta.async.AsyncConfig;
import org.terracotta.async.AsyncCoordinator;
import org.terracotta.cache.serialization.DsoSerializationStrategy;
import org.terracotta.cluster.ClusterInfo;
import org.terracotta.cluster.ClusterLogger;
import org.terracotta.cluster.TerracottaClusterInfo;
import org.terracotta.cluster.TerracottaLogger;
import org.terracotta.collections.ConcurrentDistributedMap;
import org.terracotta.locking.LockType;
import org.terracotta.locking.strategy.HashcodeLockStrategy;
import org.terracotta.modules.ehcache.LocalVMResources;
import org.terracotta.modules.ehcache.coherence.CacheShutdownHook;
import org.terracotta.modules.ehcache.event.ClusteredEventReplicator;
import org.terracotta.modules.ehcache.event.TerracottaTopologyImpl;
import org.terracotta.modules.ehcache.store.operatorevent.ClusterRejoinOperatorEventListener;
import org.terracotta.modules.ehcache.transaction.ClusteredTransactionIDFactory;
import org.terracotta.modules.ehcache.transaction.ReadCommittedClusteredSoftLockFactory;
import org.terracotta.modules.ehcache.writebehind.AsyncWriteBehind;
import org.terracotta.modules.ehcache.writebehind.WriteBehindAsyncConfig;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class TerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  private static final ClusterLogger LOGGER                              = new TerracottaLogger(
                                                                                                TerracottaClusteredInstanceFactory.class
                                                                                                    .getName());
  private static final String        DELIM                               = "|";

  public static final String         DEFAULT_CACHE_MANAGER_NAME          = "__DEFAULT__";

  private static final String        ROOT_NAME_STORE                     = "ehcache-store";
  private static final String        ROOT_NAME_EVENT_REPLICATOR          = "ehcache-event-replicator";
  private static final String        ROOT_NAME_EHCACHE_ASYNC_COORDINATOR = "ehcache-async-coordinator";
  private static final String        ROOT_NAME_UTILITIES                 = "ehcache-utilities";
  private static final String        ROOT_NAME_SOFT_LOCK_FACTORIES       = "ehcache-softlock-factories";

  private final Set<CacheStorePair>  cacheStorePairs                     = new HashSet<CacheStorePair>();
  private final Set<String>          registeredCacheManagers             = new HashSet<String>();

  /**
   * This constructor is called reflectively by TerracottaStoreHelper.newStoreFactory()
   */
  public TerracottaClusteredInstanceFactory(final TerracottaClientConfiguration tcConfig) {
    logEhcacheBuildInfo();
    CacheShutdownHook.INSTANCE.init();
  }

  private void logEhcacheBuildInfo() {
    final ProductInfo ehcacheCoreProductInfo = new ProductInfo();
    final ClusterLogger logger = new TerracottaLogger(TerracottaClusteredInstanceFactory.class.getName());
    logger.info(ehcacheCoreProductInfo.toString());
  }

  public Store createStore(final Ehcache cache) {
    final ClusteredSafeStore store = getOrCreateStore(cache);
    connectConfigurations(cache, store);
    return store;
  }

  private static String getCacheManagerName(final CacheManager cacheManager) {
    String cacheMgrName = cacheManager.getName();
    if (!cacheManager.isNamed()) {
      cacheMgrName = DEFAULT_CACHE_MANAGER_NAME;
    }

    if (cacheMgrName.endsWith(DELIM)) {
      //
      throw new CacheException("Cache manager name must not end with \"" + DELIM + "\" when terracotta clustered");
    }

    return cacheMgrName;
  }

  public WriteBehind createWriteBehind(final Ehcache cache) {
    final CacheWriterConfiguration config = cache.getCacheConfiguration().getCacheWriterConfiguration();
    final AsyncConfig asyncConfig = new WriteBehindAsyncConfig(config.getMinWriteDelay() * 1000,
                                                               config.getMaxWriteDelay() * 1000,
                                                               config.getWriteBatching(), config.getWriteBatchSize(),
                                                               cache.getCacheConfiguration()
                                                                   .getTerracottaConfiguration().isSynchronousWrites(),
                                                               config.getRetryAttempts(),
                                                               config.getRetryAttemptDelaySeconds() * 1000,
                                                               config.getRateLimitPerSecond(),
                                                               config.getWriteBehindMaxQueueSize());

    final AsyncCoordinator asyncCoordinator = getOrCreateAsyncCoordinator(cache, asyncConfig);

    return new AsyncWriteBehind(asyncCoordinator, cache, getSingletonDsoSerializationStrategy());
  }

  public CacheEventListener createEventReplicator(final Ehcache cache) {
    final String cacheMgrName = getCacheManagerName(cache.getCacheManager());
    final String cacheName = cache.getName();

    final String rootName = ROOT_NAME_EVENT_REPLICATOR + DELIM + cacheMgrName + DELIM + cacheName;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Looking up root: " + rootName);
    }

    final AtomicReference<ClusteredEventReplicator> created = new AtomicReference<ClusteredEventReplicator>();
    ClusteredEventReplicator root = Terracotta.lookupOrCreateRoot(rootName, new Callable<ClusteredEventReplicator>() {
      public ClusteredEventReplicator call() throws Exception {
        ClusteredEventReplicator eventReplicator = newEventReplicator(cache);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Instantiating new root (" + rootName + ") " + System.identityHashCode(eventReplicator));
        }
        created.set(eventReplicator);
        return eventReplicator;
      }
    });

    boolean initializeTransients = root != created.get();

    if (initializeTransients) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Initializing transients for (" + rootName + ") " + System.identityHashCode(root));
      }
      root.initializeTransients(cache);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Returning root (" + rootName + ") " + System.identityHashCode(root) + " with oid "
                   + getOidFor(root));
    }

    return root;
  }

  private static String getOidFor(Object obj) {
    try {
      Object tco = obj.getClass().getMethod("__tc_managed").invoke(obj);
      return tco.getClass().getMethod("getObjectID").invoke(tco).toString();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private ClusteredEventReplicator newEventReplicator(final Ehcache cache) {
    return new ClusteredEventReplicator(cache, cache.getCacheConfiguration().getTerracottaConfiguration());
  }

  static ClusteredStore getExistingStore(final String cacheMgrName, final String cacheName) {
    return Terracotta.lookupOrCreateRoot(storeRootName(cacheMgrName, cacheName), new Callable<ClusteredStore>() {
      public ClusteredStore call() throws Exception {
        return null;
      }
    });
  }

  private static String storeRootName(final String cacheMgrName, final String cacheName) {
    return ROOT_NAME_STORE + DELIM + cacheMgrName + DELIM + cacheName;
  }

  private ClusteredSafeStore getOrCreateStore(final Ehcache cache) {
    String cacheMgrName = getCacheManagerName(cache.getCacheManager());

    final String cacheName = cache.getName();
    if (isDCV2(cache)) {
      synchronized (cacheName.intern()) {
        // We don't want to leak the reference to the cache until the server is fully aware of it.
        return getOrCreateStoreInternal(cacheMgrName, cacheName, cache);
      }
    } else {
      return getOrCreateStoreInternal(cacheMgrName, cacheName, cache);
    }
  }

  private ClusteredSafeStore getOrCreateStoreInternal(final String cacheMgrName, final String cacheName,
                                                      final Ehcache cache) {
    // register the cacheManager in LocalVMResources
    registerLocalResourceCacheManager(cache.getCacheManager());
    // register the cache while initialization is happening
    registerLocalResourceCache(cache);

    final AtomicReference<ClusteredStore> created = new AtomicReference<ClusteredStore>();

    ClusteredStore root = Terracotta.lookupOrCreateRoot(storeRootName(cacheMgrName, cacheName),
                                                        new Callable<ClusteredStore>() {
                                                          public ClusteredStore call() throws Exception {
                                                            ClusteredStore store = newStore(cache, cacheMgrName + DELIM
                                                                                                   + cacheName);
                                                            created.set(store);
                                                            return store;
                                                          }
                                                        });

    boolean initializeTransients = root != created.get();
    if (initializeTransients) {
      root.initalizeTransients(cache);
    } else {
      waitUntilStoreCreatedInServer(cache);
    }

    // unregister the cache after initialization is complete
    unregisterLocalResourceCache(cache);
    return new ClusteredSafeStore(root);
  }

  private void unregisterLocalResourceCache(Ehcache cache) {
    LocalVMResources.getInstance().unregisterCache(cache);
  }

  private void registerLocalResourceCache(Ehcache cache) {
    Object oldCache = LocalVMResources.getInstance().registerCache(cache);
    if (oldCache != null && oldCache != cache) {
      //
      throw new CacheException("Some other object mapped for cache with name '" + cache.getName() + "', other: "
                               + oldCache);
    }
  }

  private void registerLocalResourceCacheManager(CacheManager cacheManager) {
    // use the cacheManager name to uniquely identify it across nodes
    final String uuid = getCacheManagerClusterId(cacheManager);
    Object object = LocalVMResources.getInstance().registerCacheManager(uuid, cacheManager);
    if (object != null && object != cacheManager) {
      // should be same reference if another mapping existed
      throw new CacheException(
                               "Some other object already mapped to current CacheManager's uuid: "
                                   + uuid
                                   + " object: "
                                   + object
                                   + ". Probably there are multiple cacheManagers having the same name (or not named at all). "
                                   + "Please fix to have unique names for each CacheManager by specifying it in the config");
    }
    synchronized (registeredCacheManagers) {
      registeredCacheManagers.add(uuid);
    }
  }

  private void waitUntilStoreCreatedInServer(final Ehcache ehcache) {
    if (isDCV2(ehcache)) {
      // We don't want to operate on the cache until the server is fully aware of it.
      Terracotta.waitForAllCurrentTransactionsToComplete();
    }
  }

  private boolean isDCV2(final Ehcache ehcache) {
    return StorageStrategy.DCV2.equals(ehcache.getCacheConfiguration().getTerracottaConfiguration()
        .getStorageStrategy());
  }

  protected ClusteredStore newStore(final Ehcache cache, final String qualifiedName) {
    return new ClusteredStore(cache, qualifiedName);
  }

  private String getCacheManagerClusterId(CacheManager cacheManager) {
    return getCacheManagerName(cacheManager);
  }

  private static AsyncCoordinator getOrCreateAsyncCoordinator(final Ehcache cache, final AsyncConfig config) {
    final String cacheMgrName = getCacheManagerName(cache.getCacheManager());
    final String cacheName = cache.getName();

    return Terracotta
        .lookupOrCreateRoot(ROOT_NAME_EHCACHE_ASYNC_COORDINATOR + DELIM + cacheMgrName + DELIM + cacheName,
                            new Callable<AsyncCoordinator>() {
                              public AsyncCoordinator call() throws Exception {
                                return new AsyncCoordinator(config);
                              }
                            });
  }

  private static DsoSerializationStrategy getSingletonDsoSerializationStrategy() {
    final ConcurrentMap<String, Object> root = Terracotta
        .lookupOrCreateRoot(ROOT_NAME_UTILITIES, new Callable<ConcurrentMap<String, Object>>() {
          public ConcurrentMap<String, Object> call() throws Exception {
            return new ConcurrentDistributedMap<String, Object>(LockType.WRITE, new HashcodeLockStrategy(), 8);
          }
        });
    root.putIfAbsent(DsoSerializationStrategy.class.getName(), new DsoSerializationStrategy());
    Terracotta.disableEviction(root);

    return (DsoSerializationStrategy) root.get(DsoSerializationStrategy.class.getName());
  }

  private void connectConfigurations(final Ehcache cache, final ClusteredSafeStore store) {
    cache.getCacheConfiguration().addConfigurationListener(store);
    synchronized (cacheStorePairs) {
      cacheStorePairs.add(new CacheStorePair(cache, store));
    }
    cache.getCacheConfiguration().internalSetDiskCapacity(store.getBackend().getConfig().getTargetMaxTotalCount());
    cache.getCacheConfiguration().internalSetTimeToIdle(store.getBackend().getConfig().getMaxTTISeconds());
    cache.getCacheConfiguration().internalSetTimeToLive(store.getBackend().getConfig().getMaxTTLSeconds());
    cache.getCacheConfiguration().internalSetLogging(store.getBackend().getConfig().isLoggingEnabled());
  }

  public CacheCluster getTopology() {
    return TopologyHolder.TOPOLOGY;
  }

  /**
   * Initialize-on-demand holder idiom
   */
  private static class TopologyHolder {
    private static final TerracottaTopologyImpl TOPOLOGY;

    static {
      final ClusterInfo clusterInfo = new TerracottaClusterInfo();

      TOPOLOGY = new TerracottaTopologyImpl(clusterInfo);
      try {
        TOPOLOGY.addTopologyListener(new ClusterRejoinOperatorEventListener(clusterInfo.waitUntilNodeJoinsCluster()));
      } catch (Exception e) {
        LOGGER.warn("Unable to register: " + ClusterRejoinOperatorEventListener.class.getName(), e);
      }
    }
  }

  /**
   * This is used by SampledMBeanRegistrationProvider to generate a JMX MBean ObjectName containing the client's uuid so
   * that it can be associated with the correct connection when tunneled to the L2.
   */
  public String getUUID() {
    return new TerracottaClusterInfo().getUniversallyUniqueClientID();
  }

  /**
   * This is the clustered instance factory for a custom instance it does nothing on shutdown.
   * <p>
   * It's also possible that this is the delegate for an express clustered instance factory. Even in this case we don't
   * need to do anything since the wrapping factory will shutdown the client for us.
   */
  public void shutdown() {
    synchronized (cacheStorePairs) {
      for (CacheStorePair cacheStorePair : cacheStorePairs) {
        cacheStorePair.getCache().getCacheConfiguration().removeConfigurationListener(cacheStorePair.getStore());
      }
      cacheStorePairs.clear();
    }
    synchronized (registeredCacheManagers) {
      for (String uuid : registeredCacheManagers) {
        LocalVMResources.getInstance().unregisterCacheManager(uuid);
      }
      registeredCacheManagers.clear();
    }
  }

  public TransactionIDFactory createTransactionIDFactory(String uuid, String cacheManagerName) {
    return new ClusteredTransactionIDFactory(uuid, cacheManagerName);
  }

  public SoftLockFactory getOrCreateSoftLockFactory(Ehcache cache) {
    String cacheName = cache.getName();
    String cacheManagerName = getCacheManagerName(cache.getCacheManager());
    ConcurrentMap<String, SoftLockFactory> factories = getSoftLockFactoriesRoot();

    SoftLockFactory softLockFactory = factories.get(cacheName);
    if (softLockFactory == null) {
      softLockFactory = new ReadCommittedClusteredSoftLockFactory(cacheManagerName, cacheName);
      SoftLockFactory old = factories.putIfAbsent(cacheName, softLockFactory);
      if (old != null) {
        softLockFactory = old;
      }
    }

    return softLockFactory;
  }

  private static ConcurrentMap<String, SoftLockFactory> getSoftLockFactoriesRoot() {
    final ConcurrentMap<String, SoftLockFactory> root = Terracotta
        .lookupOrCreateRoot(ROOT_NAME_SOFT_LOCK_FACTORIES, new Callable<ConcurrentMap<String, SoftLockFactory>>() {
          public ConcurrentMap<String, SoftLockFactory> call() throws Exception {
            return new ConcurrentDistributedMap<String, SoftLockFactory>();
          }
        });
    Terracotta.disableEviction(root);

    return root;
  }

  private static class CacheStorePair {
    private final Ehcache            cache;
    private final ClusteredSafeStore store;

    private CacheStorePair(Ehcache cache, ClusteredSafeStore store) {
      this.cache = cache;
      this.store = store;
    }

    public Ehcache getCache() {
      return cache;
    }

    public ClusteredSafeStore getStore() {
      return store;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof CacheStorePair)) { return false; }
      CacheStorePair otherPair = (CacheStorePair) obj;

      return cache.getName().equals(otherPair.cache.getName());
    }

    @Override
    public int hashCode() {
      return cache.getName().hashCode();
    }
  }

}
