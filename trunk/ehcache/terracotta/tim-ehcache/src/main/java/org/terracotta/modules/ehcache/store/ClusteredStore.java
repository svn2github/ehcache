/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.FifoPolicy;
import net.sf.ehcache.store.LfuPolicy;
import net.sf.ehcache.store.LruPolicy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.util.lang.VicariousThreadLocal;
import net.sf.ehcache.writer.CacheWriterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.api.Terracotta;
import org.terracotta.cache.MutableConfig;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.cache.evictor.CapacityEvictionPolicyData;
import org.terracotta.cache.evictor.LFUCapacityEvictionPolicyData;
import org.terracotta.cache.evictor.LRUCapacityEvictionPolicyData;
import org.terracotta.cache.logging.ConfigChangeListener;
import org.terracotta.cluster.ClusterLogger;
import org.terracotta.cluster.TerracottaLogger;
import org.terracotta.cluster.TerracottaProperties;
import org.terracotta.collections.ClusteredMap;
import org.terracotta.collections.ClusteredMapConfigFields;
import org.terracotta.collections.ConcurrentDistributedServerMap;
import org.terracotta.config.Configuration;
import org.terracotta.locking.ClusteredLock;
import org.terracotta.locking.LockType;
import org.terracotta.meta.MetaData;
import org.terracotta.modules.ehcache.coherence.CacheCoherence;
import org.terracotta.modules.ehcache.coherence.CacheShutdownHook;
import org.terracotta.modules.ehcache.coherence.IncoherentNodesSet;
import org.terracotta.modules.ehcache.concurrency.TcCacheLockProvider;
import org.terracotta.modules.ehcache.store.backend.BackendStore;
import org.terracotta.modules.ehcache.store.backend.BulkLoadBackend;
import org.terracotta.modules.ehcache.store.backend.NonStrictBackend;
import org.terracotta.modules.ehcache.store.backend.StrictBackend;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.event.EventListenerList;

public class ClusteredStore implements TerracottaStore, CacheConfigurationListener, ConfigChangeListener,
    ClusteredMapConfigFields {

  private final static Policy                                                   LFU_POLICY_INSTANCE       = new LfuPolicy();
  private final static Policy                                                   LRU_POLICY_INSTANCE       = new LruPolicy();
  private static final Logger                                                   LOG                       = LoggerFactory
                                                                                                              .getLogger(ClusteredStore.class
                                                                                                                  .getName());
  private static final ClusterLogger                                            TC_LOGGER                 = new TerracottaLogger(
                                                                                                                                 ClusteredStore.class
                                                                                                                                     .getName());
  private static final TerracottaProperties                                     TC_PROPERTIES             = new TerracottaProperties();
  private static final String                                                   CHECK_CONTAINS_KEY_ON_PUT = "ehcache.clusteredStore.checkContainsKeyOnPut";

  private static final ThreadLocal<SyncLockState>                               syncLockState             = new VicariousThreadLocal<SyncLockState>() {
                                                                                                            @Override
                                                                                                            protected SyncLockState initialValue() {
                                                                                                              return SyncLockState.UNLOCKED;
                                                                                                            }
                                                                                                          };

  // Clustered references
  private final ClusteredStoreBackend<Object, Object>                           backend;
  protected final ValueModeHandler                                              valueModeHandler;
  private final int                                                             localKeyCacheMaxsize;
  private final CacheCoherence                                                  cacheCoherence;
  protected final String                                                        qualifiedCacheName;
  private final boolean                                                         isIdentity;
  private final boolean                                                         isClassic;
  private final Consistency                                                     initialCoherenceMode;
  private final CacheConfiguration.TransactionalMode                            transactionalMode;

  // Unclustered references, initialized in initializeTransients()
  /**
   * The cache this store is associated with.
   */
  protected transient Ehcache                                                   cache;
  private transient volatile Map<Object, Object>                                keyLookupCache;
  private transient volatile Set<CacheConfiguration>                            linkedConfigurations      = new CopyOnWriteArraySet<CacheConfiguration>();
  private transient volatile LocalBufferedMap<Object, TimestampedValue<Object>> localBufferedMap;
  private transient volatile boolean                                            checkContainsKeyOnPut;
  private transient EventListenerList                                           listenerList;
  private transient NonStrictBackend                                            nonStrictBackend;
  private transient StrictBackend                                               strictBackend;
  private transient BulkLoadBackend                                             bulkLoadBackend;
  private transient boolean                                                     cachePinned;
  private final boolean                                                         localCacheEnabled;

  public ClusteredStore(final Ehcache cache, long uniqueID) {
    // appending a unique identifier to the cache name is necessary to avoid collisions when the cache name starts with
    // "_" or if the name contains characters not legal for use as a filesystem path (ie. L2 lucene index path)
    this.qualifiedCacheName = cache.getCacheManager().getName() + "_" + cache.getName() + "_" + uniqueID;

    final CacheConfiguration ehcacheConfig = cache.getCacheConfiguration();
    checkMemoryStoreEvictionPolicy(ehcacheConfig);
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();

    if (terracottaConfiguration == null || !terracottaConfiguration.isClustered()) { throw new IllegalArgumentException(
                                                                                                                        "Cannot create clustered store for non-terracotta clustered caches"); }
    localCacheEnabled = terracottaConfiguration.isLocalCacheEnabled();
    initialCoherenceMode = terracottaConfiguration.getConsistency();
    transactionalMode = ehcacheConfig.getTransactionalMode();

    if (ehcacheConfig.isOverflowToDisk()) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Persistence on disk on the local node is not supported with a Terracotta clustered ehcache store. Configure the Terracotta server array to be persistent instead.");
      }
    }

    valueModeHandler = ValueModeHandlerFactory.createValueModeHandler(this, ehcacheConfig);
    cachePinned = cache.getCacheConfiguration().getPinningConfiguration() != null
                  && cache.getCacheConfiguration().getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;

    if (terracottaConfiguration.getLocalKeyCache()) {
      localKeyCacheMaxsize = terracottaConfiguration.getLocalKeyCacheSize();
      keyLookupCache = new ConcurrentHashMap<Object, Object>();
    } else {
      localKeyCacheMaxsize = -1;
      keyLookupCache = null;
    }

    final Configuration clusteredMapConfig = createClusteredMapConfig(cache);
    ClusteredMap<Object, TimestampedValue<Object>> map = createConcurrentDistributedMap(cache, clusteredMapConfig);
    this.backend = new ClusteredStoreBackendImpl(clusteredMapConfig, map, valueModeHandler,
                                                 cache.getCacheEventNotificationService(), qualifiedCacheName, this);
    cacheCoherence = new IncoherentNodesSet(cache.getName(), this);

    initalizeTransients(cache);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
    }

    this.isIdentity = terracottaConfiguration.getValueMode() == ValueMode.IDENTITY;
    this.isClassic = StorageStrategy.CLASSIC.equals(terracottaConfiguration.getStorageStrategy());
  }

  private void checkMemoryStoreEvictionPolicy(final CacheConfiguration config) {
    MemoryStoreEvictionPolicy policy = config.getMemoryStoreEvictionPolicy();
    if (policy == MemoryStoreEvictionPolicy.FIFO || policy == MemoryStoreEvictionPolicy.LFU) { throw new IllegalArgumentException(
                                                                                                                                  "Policy '"
                                                                                                                                      + policy
                                                                                                                                      + "' is not a supported memory store eviction policy."); }
  }

  private Configuration createClusteredMapConfig(Ehcache ehcache) {
    final CacheConfiguration ehcacheConfig = ehcache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    final ClusteredMapConfigurationBuilder builder = new ClusteredMapConfigurationBuilder();

    builder.maxTTI((int) ehcacheConfig.getTimeToIdleSeconds());
    builder.maxTTL((int) ehcacheConfig.getTimeToLiveSeconds());
    builder.name(qualifiedCacheName);
    builder.maxTotalCount(cachePinned ? 0 : ehcacheConfig.getMaxElementsOnDisk());
    builder.localCacheEnabled(localCacheEnabled);

    if (terracottaConfiguration.isSynchronousWrites()) {
      builder.lockType(LockType.SYNCHRONOUS_WRITE);
    } else {
      builder.lockType(LockType.WRITE);
    }

    if (terracottaConfiguration.getConcurrency() == TerracottaConfiguration.DEFAULT_CONCURRENCY) {
      builder.concurrency(calculateCorrectConcurrency(ehcacheConfig));
    } else {
      builder.concurrency(terracottaConfiguration.getConcurrency());
    }
    builder.invalidateOnChange(terracottaConfiguration.getConsistency() == Consistency.EVENTUAL);

    final String cmName = ehcache.getCacheManager().isNamed() ? ehcache.getCacheManager().getName()
        : TerracottaClusteredInstanceFactory.DEFAULT_CACHE_MANAGER_NAME;
    builder.localStoreManager(cmName);
    builder.localStoreName(ehcache.getName());
    builder.localStoreUUID(ehcache.getGuid());

    if (ehcacheConfig.getPinningConfiguration() != null) {
      builder.pinningStore(ehcacheConfig.getPinningConfiguration().getStore());
    }

    if (ehcacheConfig.getMaxEntriesLocalHeap() > 0) {
      builder.maxEntriesLocalHeap((int) ehcacheConfig.getMaxEntriesLocalHeap());
    }

    if (ehcacheConfig.getMaxBytesLocalHeap() > 0) {
      builder.maxBytesLocalHeap(ehcacheConfig.getMaxBytesLocalHeap());
    }

    if (ehcacheConfig.getMaxBytesLocalOffHeap() > 0) {
      builder.maxBytesLocalOffheap(ehcacheConfig.getMaxBytesLocalOffHeap());
    }

    builder.overflowToOffheap(ehcacheConfig.isOverflowToOffHeap());

    return builder.build();
  }

  protected ClusteredMap<Object, TimestampedValue<Object>> createConcurrentDistributedMap(Ehcache ehcache,
                                                                                          Configuration clusteredMapConfig) {
    ClusteredMap<Object, TimestampedValue<Object>> clusteredMap = new ConcurrentDistributedServerMap(clusteredMapConfig);

    LOG.info(getConcurrencyValueLogMsg(ehcache.getName(), clusteredMapConfig.getInt(CONCURRENCY_FIELD_NAME)));
    return clusteredMap;
  }

  private int calculateCorrectConcurrency(CacheConfiguration cacheConfiguration) {
    int maxElementOnDisk = cacheConfiguration.getMaxElementsOnDisk();
    if (maxElementOnDisk <= 0 || maxElementOnDisk >= 256) { return ConcurrentDistributedServerMap.SERVERMAP_DEFAULT_CONCURRENCY; }
    int concurrency = 1;
    while (concurrency * 2 <= maxElementOnDisk) {// this while loop is not very time consuming, maximum it will do 8
                                                 // iterations
      concurrency *= 2;
    }
    return concurrency;
  }

  // tests assert on the log msg printed
  static String getConcurrencyValueLogMsg(String name, int concurrency) {
    return "Cache [" + name + "] using concurrency: " + concurrency;
  }

  ClusteredStoreBackend getBackend() {
    return backend;
  }

  public Consistency getInitialCoherenceMode() {
    return initialCoherenceMode;
  }

  public CacheConfiguration.TransactionalMode getTransactionalMode() {
    return transactionalMode;
  }

  public boolean isIdentity() {
    return isIdentity;
  }

  /**
   * This method is used for common on-load and instantiation logic. We can't rely on the standard DSO on-load feature
   * since roots of ClusteredStore are explicitly created in
   * {@link org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory} through ManagerUtil call, as
   * opposed to the regular root declaration in DSO instrumented classes.
   * <p/>
   * This approach is needed for 'express' features since none of the application context classes can be instrumented by
   * Terracotta due to the lack of a boot jar.
   */
  public void initalizeTransients(final Ehcache ehcache) {
    this.cache = ehcache;
    cachePinned = cache.getCacheConfiguration().getPinningConfiguration() != null
                  && cache.getCacheConfiguration().getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;
    if (localKeyCacheMaxsize > 0) {
      keyLookupCache = new ConcurrentHashMap<Object, Object>();
    } else {
      keyLookupCache = null;
    }
    this.linkedConfigurations = new CopyOnWriteArraySet<CacheConfiguration>();
    ((MutableConfig) backend.getConfig()).addConfigChangeListener(this);
    backend.initializeTransients(cache.getCacheEventNotificationService(), this);
    if (localBufferedMap == null) {
      localBufferedMap = new LocalBufferedMap<Object, TimestampedValue<Object>>(backend, cacheCoherence,
                                                                                valueModeHandler);
    }
    if (StorageStrategy.DCV2.equals(cache.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy())) {
      // use false by default
      checkContainsKeyOnPut = TC_PROPERTIES.getBoolean(CHECK_CONTAINS_KEY_ON_PUT, false);
    } else {
      // use true by default
      checkContainsKeyOnPut = TC_PROPERTIES.getBoolean(CHECK_CONTAINS_KEY_ON_PUT, true);
    }
    CacheShutdownHook.INSTANCE.registerCache(cache);

    strictBackend = new StrictBackend(this, backend, valueModeHandler, cacheCoherence);
    bulkLoadBackend = new BulkLoadBackend(this, backend, valueModeHandler, localBufferedMap, cacheCoherence);
    nonStrictBackend = new NonStrictBackend(this, backend, valueModeHandler, syncLockState, cacheCoherence,
                                            SizeOfPolicyConfiguration.resolveMaxDepth(cache), SizeOfPolicyConfiguration
                                                .resolveBehavior(cache)
                                                .equals(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.ABORT));

    TC_LOGGER.info("Clustered Store [cache=" + ehcache.getName() + "] with checkContainsKeyOnPut: "
                   + checkContainsKeyOnPut);
    TC_LOGGER.info("Clustered Store [cache=" + ehcache.getName() + "] with storageStrategy: "
                   + ehcache.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy().name());

    // fault in references
    backend.initializeLocalCache(createClusteredMapConfig(ehcache));
    valueModeHandler.loadReferences();
    cacheCoherence.loadReferences();
  }

  public boolean put(final Element element) throws CacheException {
    return putInternal(element, null);
  }

  public void putAll(final Collection<Element> elements) {
    getCurrentBackendStore().putAllNoReturn(elements);
  }

  public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
    return putInternal(element, writerManager);
  }

  private boolean putInternal(Element element, CacheWriterManager writerManager) throws CacheException {
    if (element != null) {
      Object pKey = generatePortableKeyFor(element.getObjectKey());

      cacheCoherence.acquireReadLock();
      try {
        MetaData searchMetaData = createPutSearchMetaData(pKey, element);

        final boolean takeLock = cacheCoherence.isNodeCoherent() && writerManager != null;
        ClusteredLock lock = null;
        if (takeLock) {
          lock = backend.createFinegrainedLock(pKey);
          lock.lock();
        }
        try {
          boolean newPut = checkContainsKeyOnPut ? !internalContainsKey(pKey) : true;
          // Keep this before the backend put to ensure that a write behind operation can never be lost.
          // This will be handled in the Terracotta L2 as an atomic operation right after the backend put since at that
          // time a lock commit is issued that splits up the transaction. It will not be visible before either since
          // there are no other lock boundaries in the code path.
          if (writerManager != null) {
            writerManager.put(element);
          }
          TimestampedValue value = valueModeHandler.createTimestampedValue(element);

          doPut(pKey, value, searchMetaData);

          // TODO: once the setElementEvictionData is called when the localBufferedMap is flushed to server, the part
          // below should move within the 'coherent' section of the if statement above
          // Since the element is now inside the clustered store, replace its eviction data with a clustered version
          element.setElementEvictionData(new ClusteredElementEvictionData(this, value));
          return newPut;
        } finally {
          if (takeLock) {
            lock.unlock();
          }
        }
      } finally {
        cacheCoherence.releaseReadLock();
      }
    } else {
      return true;
    }
  }

  private BackendStore getCurrentBackendStore() {
    if (!cacheCoherence.isNodeCoherent()) { return bulkLoadBackend; }
    switch (this.initialCoherenceMode) {
      case STRONG:
        return strictBackend;
      case EVENTUAL:
        return nonStrictBackend;
      default:
        throw new IllegalStateException("Unknown consistency: " + initialCoherenceMode);
    }
  }

  private void doPut(Object portableKey, TimestampedValue value, MetaData searchMetaData) {
    getCurrentBackendStore().putNoReturn(portableKey, value, searchMetaData);
  }

  public MetaData createPutSearchMetaData(Object portableKey, Element element) {
    // implemented in EE subclass only
    return null;
  }

  public MetaData createRemoveSearchMetaData(Object key) {
    // implemented in EE subclass only
    return null;
  }

  protected MetaData createClearSearchMetaData() {
    // implemented in EE subclass only
    return null;
  }

  public Element get(final Object key) {
    return doGet(key, false);
  }

  public Element getQuiet(final Object key) {
    return doGet(key, true);
  }

  private Element doGet(final Object key, final boolean quiet) {
    if (key == null) { return null; }

    Object pKey = generatePortableKeyFor(key);
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().get(key, pKey, quiet);
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public Map<Object, Element> getAllQuiet(Collection<?> keys) {
    return doGetAll(keys, true);
  }

  public Map<Object, Element> getAll(Collection<?> keys) {
    return doGetAll(keys, false);
  }

  private Map<Object, Element> doGetAll(Collection<?> keys, boolean quiet) {
    return getCurrentBackendStore().getAll(keys, quiet);
  }

  public Element unlockedGet(final Object key) {
    return doUnlockedGet(key, false);
  }

  public Element unlockedGetQuiet(final Object key) {
    return doUnlockedGet(key, true);
  }

  private Element doUnlockedGet(final Object key, final boolean quiet) {
    if (key == null) { return null; }
    Object pKey = generatePortableKeyFor(key);
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().unlockedGet(key, pKey, quiet);
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public Element unsafeGetQuiet(final Object key) {
    return doUnsafeGet(key, true);
  }

  public Element unsafeGet(final Object key) {
    return doUnsafeGet(key, false);
  }

  private Element doUnsafeGet(final Object key, final boolean quiet) {
    if (key == null) { return null; }
    Object pKey = generatePortableKeyFor(key);
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().unsafeGet(key, pKey, quiet);
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public void removeAll(Collection<?> keys) {
    getCurrentBackendStore().removeAll(keys, keyLookupCache);
  }

  public Element remove(final Object key) {
    if (key == null) { return null; }
    // remove single item.
    Element element = removeFromBackend(key);
    if (element != null) {
      return element;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug(cache.getName() + " Cache: Cannot remove entry as key " + key + " was not found");
      }
      return null;
    }
  }

  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {

    if (key == null) { return null; }

    Object pKey = generatePortableKeyFor(key);
    ClusteredLock lock = backend.createFinegrainedLock(pKey);
    lock.lock();
    try {
      // Keep this before the backend remove to ensure that a write behind operation can never be lost.
      // This will be handled in the Terracotta L2 as an atomic operation right after the backend remove since at that
      // time a lock commit is issued that splits up the transaction. It will not be visible before either since there
      // are no other lock boundaries in the code path.
      if (writerManager != null) {
        writerManager.remove(new CacheEntry(key, get(key)));
      }

      // Remove the value from the clustered backend.
      Element element = removeFromBackend(key);

      if (element != null) {
        return element;
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug(cache.getName() + " Cache: Cannot remove entry as key " + key + " was not found");
        }
        return null;
      }

    } finally {
      lock.unlock();
    }
  }

  protected Element removeFromBackend(final Object key) {
    Object pKey = generatePortableKeyFor(key);
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().remove(key, pKey, createRemoveSearchMetaData(pKey));
    } finally {
      if (keyLookupCache != null) {
        keyLookupCache.remove(key);
      }
      cacheCoherence.releaseReadLock();
    }
  }

  /**
   * Memory stores are never backed up and always return false
   */
  public final boolean bufferFull() {
    return false;
  }

  /**
   * Expire all elements.
   * <p/>
   * This is a default implementation which does nothing. Expiration on demand is only implemented for disk stores.
   */
  public final void expireElements() {
    // empty implementation
  }

  /**
   * Chooses the Policy from the cache configuration
   * 
   * @param cache
   */
  protected static final Policy determineEvictionPolicy(Ehcache cache) {
    MemoryStoreEvictionPolicy policySelection = cache.getCacheConfiguration().getMemoryStoreEvictionPolicy();

    if (policySelection.equals(MemoryStoreEvictionPolicy.LRU)) {
      return new LruPolicy();
    } else if (policySelection.equals(MemoryStoreEvictionPolicy.FIFO)) {
      return new FifoPolicy();
    } else if (policySelection.equals(MemoryStoreEvictionPolicy.LFU)) { return new LfuPolicy(); }

    throw new IllegalArgumentException(policySelection + " isn't a valid eviction policy");
  }

  public boolean containsKey(final Object key) {
    cacheCoherence.acquireReadLock();
    try {
      return internalContainsKey(generatePortableKeyFor(key));
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public boolean containsLocalKey(final Object key) {
    cacheCoherence.acquireReadLock();
    try {
      return internalContainsLocalKey(generatePortableKeyFor(key));
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public boolean containsKeyInMemory(Object key) {
    Object portableKey = generatePortableKeyFor(key);
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().containsKeyLocalOnHeap(portableKey);
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public boolean containsKeyOnDisk(Object key) {
    return false;
  }

  private boolean internalContainsKey(Object internalKey) {
    return getCurrentBackendStore().containsKey(internalKey);
  }

  private boolean internalContainsLocalKey(Object internalKey) {
    return getCurrentBackendStore().containsLocalKey(internalKey);
  }

  public int getSize() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getSize();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public int getInMemorySize() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getLocalOnHeapSize();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public long getInMemorySizeInBytes() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getLocalHeapSizeInBytes();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public int getOnDiskSize() {
    return 0;
  }

  public long getOnDiskSizeInBytes() {
    return 0;
  }

  public boolean hasAbortedSizeOf() {
    return false;
  }

  public int getTerracottaClusteredSize() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getTerracottaClusteredSize();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public void removeAll() throws CacheException {
    clear();
  }

  public Status getStatus() {
    return Status.STATUS_ALIVE;
  }

  protected final void clear() {
    cacheCoherence.acquireWriteLock();
    try {
      getCurrentBackendStore().clear(createClearSearchMetaData());

      if (keyLookupCache != null) {
        keyLookupCache.clear();
      }
    } finally {
      cacheCoherence.releaseWriteLock();
    }
  }

  public void flush() {
    // should be emptied if clearOnFlush is true
    if (cache.getCacheConfiguration().isClearOnFlush()) {
      clear();
    }
  }

  public void dispose() {
    try {
      if (cacheCoherence.isClusterOnline()) {
        cacheCoherence.acquireWriteLock();
        try {
          localBufferedMap.dispose();
        } finally {
          cacheCoherence.releaseWriteLock();
        }
      } else {
        localBufferedMap.shutdown();
      }
    } finally {
      try {
        backend.shutdown();
        cacheCoherence.dispose();
      } finally {
        CacheShutdownHook.INSTANCE.unregisterCache(cache);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public List getKeys() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getKeys();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  /**
   * {@inheritDoc}
   */
  public Set getLocalKeys() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getLocalKeys();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  /**
   * Generates a portable key for the supplied object.
   */
  public Object generatePortableKeyFor(final Object obj) {
    boolean useCache = shouldUseCache(obj);

    if (useCache) {
      Object value = keyLookupCache.get(obj);
      if (value != null) { return value; }
    }

    Object key;
    try {
      key = this.valueModeHandler.createPortableKey(obj);
    } catch (Exception e) {
      throw new CacheException(e);
    }

    if (useCache && keyLookupCache.size() < localKeyCacheMaxsize) {
      keyLookupCache.put(obj, key);
    }

    return key;
  }

  private boolean shouldUseCache(final Object obj) {
    // no sense putting existing String keys into the soft cache
    return keyLookupCache != null && !(obj instanceof String);
  }

  public Policy getInMemoryEvictionPolicy() {
    CapacityEvictionPolicyData.Factory factory = backend.getConfig().getCapacityEvictionPolicyDataFactory();
    if (factory instanceof LFUCapacityEvictionPolicyData.Factory) {
      return LFU_POLICY_INSTANCE;
    } else if (factory instanceof LRUCapacityEvictionPolicyData.Factory) { return LRU_POLICY_INSTANCE; }

    throw new AssertionError(
                             "An instance of "
                                 + factory
                                 + " isn't supposed to be set in the config of the clustered store as it's not understood by Ehcache");
  }

  public void setInMemoryEvictionPolicy(final Policy policy) {
    backend.getConfig().setCapacityEvictionPolicyDataFactory(determineCapacityEvictionPolicyDataFactory(policy));
  }

  public Object getInternalContext() {
    return new TcCacheLockProvider(syncLockState, backend, valueModeHandler);
  }

  private static CapacityEvictionPolicyData.Factory determineCapacityEvictionPolicyDataFactory(final Policy policy) {
    if (LfuPolicy.NAME.equals(policy.getName())) {
      return new LFUCapacityEvictionPolicyData.Factory();
    } else if (LruPolicy.NAME.equals(policy.getName())) {
      return new LRUCapacityEvictionPolicyData.Factory();
    } else {
      throw new IllegalArgumentException("Cache eviction policy " + policy.getName()
                                         + " isn't supported by the clustered store.");
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isCacheCoherent() {
    return isClusterCoherent();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isClusterCoherent() {
    return cacheCoherence.isClusterCoherent();
  }

  /**
   * returns true if the cache is coherent locally, or false otherwise This does not consider the state of the other
   * nodes.
   */
  public boolean isNodeCoherent() {
    return cacheCoherence.isNodeCoherent();
  }

  public void timeToIdleChanged(long oldTti, long newTti) {
    backend.setMaxTTI((int) newTti);
  }

  public void timeToLiveChanged(long oldTtl, long newTtl) {
    backend.setMaxTTL((int) newTtl);
  }

  public void diskCapacityChanged(int oldCapacity, int newCapacity) {
    if (!cachePinned) {
      backend.setTargetMaxTotalCount(newCapacity);
    }
  }

  public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
    backend.setTargetMaxInMemoryCount(newCapacity);
  }

  public void loggingChanged(boolean oldValue, boolean newValue) {
    backend.setLoggingEnabled(newValue);
  }

  public void registered(CacheConfiguration config) {
    linkedConfigurations.add(config);
  }

  public void deregistered(CacheConfiguration config) {
    linkedConfigurations.remove(config);
  }

  /**
   * {@inheritDoc}
   */
  public void maxBytesLocalHeapChanged(final long oldValue, final long newValue) {
    backend.setMaxBytesLocalHeap(newValue);
  }

  /**
   * {@inheritDoc}
   */
  public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
    // not supported
  }

  public void configChanged(String cacheName, String configName, Object oldValue, Object newValue) {
    final Set<CacheConfiguration> configs = linkedConfigurations;
    if (configs == null) {
      // DMI arrived early or this instance is inappropriately in memory (ie. faulted by not initialized)
      TC_LOGGER.info("config changed but no linked configurations present [" + cacheName + " " + configName + "]");
      return;
    }

    if (MutableConfig.MAX_TTI_SECONDS.equals(configName)) {
      for (CacheConfiguration c : configs) {
        c.internalSetTimeToIdle(((Number) newValue).longValue());
      }
    } else if (MutableConfig.MAX_TTL_SECONDS.equals(configName)) {
      for (CacheConfiguration c : configs) {
        c.internalSetTimeToLive(((Number) newValue).longValue());
      }
    } else if (MutableConfig.TARGET_MAX_ENTRIES_LOCAL_HEAP.equals(configName)) {
      for (CacheConfiguration c : configs) {
        c.internalSetMemCapacity(((Number) newValue).intValue());
      }
    } else if (MutableConfig.TARGET_MAX_TOTAL_COUNT.equals(configName)) {
      for (CacheConfiguration c : configs) {
        c.internalSetDiskCapacity(((Number) newValue).intValue());
      }
    } else if (MutableConfig.LOGGING_ENABLED.equals(configName)) {
      for (CacheConfiguration c : configs) {
        c.internalSetLogging(((Boolean) newValue).booleanValue());
      }
    } else {
      LOG.error("changing " + configName + " dynamically is not allwoed");
    }
  }

  public void setNodeCoherent(boolean coherent) {
    if (!coherent) {
      localBufferedMap.startThreadIfNecessary();
    }

    cacheCoherence.acquireWriteLock();
    boolean oldValue;
    try {
      oldValue = isNodeCoherent();
      if (coherent != oldValue) {
        if (coherent) {
          localBufferedMap.flushAndStopBuffering();
        } else {
          localBufferedMap.startBuffering();
          // clear and disable local cache while bulk-load is on
          backend.clearLocalCache();
          backend.setLocalCacheEnabled(false);
        }
        // call cacheCoherence.setCoherent(true) after stopping local buffer, so that local buffer is flushed to
        // backend.
        // calling cacheCoherence.setCoherent(true) waits untill all pending tc txns are flushed
        // above write lock acquire will block all put/get until setCoherent returns,
        // we can change this behavior later if we want ClusteredStore.setCoherent(true) to return immediately without
        // any
        // guarantee that changes done in incoherent mode will be flushed

        if (!oldValue && coherent) {
          // going from incoherent to coherent, disabling bulk-load
          backend.clearLocalCache();
          Terracotta.waitForAllCurrentTransactionsToComplete();
          // enable local cache after bulk-load is complete
          backend.setLocalCacheEnabled(true);
        }
        cacheCoherence.setNodeCoherent(coherent);
      }
    } finally {
      cacheCoherence.releaseWriteLock();
    }
  }

  public void waitUntilClusterCoherent() {
    cacheCoherence.waitUntilClusterCoherent();
  }

  private synchronized EventListenerList getEventListenerList() {
    if (listenerList == null) {
      listenerList = new EventListenerList();
    }
    return listenerList;
  }

  public synchronized void addStoreListener(StoreListener listener) {
    removeStoreListener(listener);
    getEventListenerList().add(StoreListener.class, listener);
  }

  public synchronized void removeStoreListener(StoreListener listener) {
    getEventListenerList().remove(StoreListener.class, listener);
  }

  public void fireClusterCoherent(final boolean clusterCoherent) {
    Object[] listeners = getEventListenerList().getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == StoreListener.class) {
        ((StoreListener) listeners[i + 1]).clusterCoherent(clusterCoherent);
      }
    }
  }

  public Element putIfAbsent(Element element) {
    Object pKey = generatePortableKeyFor(element.getObjectKey());
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().putIfAbsent(pKey, element, createPutSearchMetaData(pKey, element));
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public Element removeElement(Element element, ElementValueComparator comparator) {
    Object pKey = generatePortableKeyFor(element.getObjectKey());
    cacheCoherence.acquireReadLock();
    try {
      Element removedElement = null;
      try {
        removedElement = getCurrentBackendStore().removeElement(pKey, element, comparator,
                                                                createRemoveSearchMetaData(pKey));
      } finally {
        if (removedElement != null) {
          if (keyLookupCache != null) {
            keyLookupCache.remove(element.getObjectKey());
          }
        }
      }
      return removedElement;
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public boolean replace(Element old, Element element, ElementValueComparator comparator) {
    Object pKey = generatePortableKeyFor(element.getObjectKey());
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().replace(pKey, old, element, comparator, createPutSearchMetaData(pKey, element));
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public Element replace(Element element) {
    Object pKey = generatePortableKeyFor(element.getObjectKey());
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().replace(pKey, element, createPutSearchMetaData(pKey, element));
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public int getOffHeapSize() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getLocalOffHeapSize();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public long getOffHeapSizeInBytes() {
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().getOffHeapSizeInBytse();
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public boolean containsKeyOffHeap(Object key) {
    Object portableKey = generatePortableKeyFor(key);
    cacheCoherence.acquireReadLock();
    try {
      return getCurrentBackendStore().containsKeyLocalOffHeap(portableKey);
    } finally {
      cacheCoherence.releaseReadLock();
    }
  }

  public Object getMBean() {
    return null;
  }

  public boolean isSearchable() {
    return false;
  }

  public boolean isClassic() {
    return isClassic;
  }

  public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
    if (!extractors.isEmpty()) { throw new CacheException("Search attributes only supported in enterprise edition"); }
  }

  public Results executeQuery(StoreQuery query) {
    throw new UnsupportedOperationException("Search execution unsupported in non-enterprise edition");
  }

  public <T> Attribute<T> getSearchAttribute(String attributeName) {
    return null;
  }

  public int getPinnedCount() {
    // TODO this needs to be implemented when we do the ClusteredStore pinning
    return 0;
  }

  public static enum SyncLockState {
    LOCKED() {
      @Override
      public boolean isLocked() {
        return true;
      }

      @Override
      public SyncLockState lockAcquired() {
        return LOCKED;
      }

      @Override
      public SyncLockState lockReleased() {
        return UNLOCKED;
      }
    },
    UNLOCKED() {
      @Override
      public boolean isLocked() {
        return false;
      }

      @Override
      public SyncLockState lockAcquired() {
        return LOCKED;
      }

      @Override
      public SyncLockState lockReleased() {
        return UNLOCKED;
      }
    };
    public abstract boolean isLocked();

    public abstract SyncLockState lockAcquired();

    public abstract SyncLockState lockReleased();
  }

  public void unpinAll() {
    if (!localCacheEnabled) { throw new UnsupportedOperationException(
                                                                      "unpinAll is not supported when local cache is disabled"); }
    backend.unpinAll();
  }

  public boolean isPinned(Object key) {
    if (!localCacheEnabled) { throw new UnsupportedOperationException(
                                                                      "Pinning is not supported when local cache is disabled"); }
    return backend.isPinned(key);
  }

  public void setPinned(Object key, boolean pinned) {
    if (!localCacheEnabled) { throw new UnsupportedOperationException(
                                                                      "Pinning is not supported when local cache is disabled"); }
    backend.setPinned(key, pinned);
  }

  public void recalculateSize(Object key) {
    throw new UnsupportedOperationException("Recalculate size is not supported for Terracotta clustered caches.");
  }

}
