/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.ElementData;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.ConfigError;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.concurrency.TCCacheLockProvider;
import org.terracotta.toolkit.ToolkitProperties;
import org.terracotta.toolkit.collections.ToolkitCacheListener;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata.EntryWithMetaData;
import org.terracotta.toolkit.internal.meta.MetaData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.event.EventListenerList;

public class ClusteredStore implements TerracottaStore {

  private static final Logger                                    LOG                                     = LoggerFactory
                                                                                                             .getLogger(ClusteredStore.class
                                                                                                                 .getName());
  private static final String                                    CHECK_CONTAINS_KEY_ON_PUT_PROPERTY_NAME = "ehcache.clusteredStore.checkContainsKeyOnPut";

  // final protected fields
  protected final ToolkitCacheWithMetadata<Object, Serializable> backend;
  protected final ValueModeHandler                               valueModeHandler;
  protected final ToolkitInstanceFactory                         toolkitInstanceFactory;
  protected final Ehcache                                        cache;
  protected final String                                         fullyQualifiedCacheName;

  // final private fields
  private final boolean                                          checkContainsKeyOnPut;
  private final int                                              localKeyCacheMaxsize;
  private final CacheConfiguration.TransactionalMode             transactionalMode;
  private final Map                                              keyLookupCache;
  private final CacheConfigChangeBridge                          cacheConfigChangeBridge;
  private final RegisteredEventListeners                         registeredEventListeners;
  private final TCCacheLockProvider                              cacheLockProvider;

  // non-final private fields
  private EventListenerList                                      listenerList;

  public ClusteredStore(ToolkitInstanceFactory toolkitInstanceFactory, Ehcache cache) {
    validateConfig(cache);
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.cache = cache;
    this.fullyQualifiedCacheName = toolkitInstanceFactory.getFullyQualifiedCacheName(cache);
    final CacheConfiguration ehcacheConfig = cache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();

    transactionalMode = ehcacheConfig.getTransactionalMode();
    valueModeHandler = ValueModeHandlerFactory.createValueModeHandler(this, ehcacheConfig);

    if (terracottaConfiguration.getLocalKeyCache()) {
      localKeyCacheMaxsize = terracottaConfiguration.getLocalKeyCacheSize();
      keyLookupCache = new ConcurrentHashMap<Object, Object>();
    } else {
      localKeyCacheMaxsize = -1;
      keyLookupCache = null;
    }

    checkContainsKeyOnPut = isCheckContainsKeyOnPut(toolkitInstanceFactory, cache);
    backend = toolkitInstanceFactory.getOrCreateToolkitCache(cache);
    // connect configurations
    cacheConfigChangeBridge = createConfigChangeBridge(toolkitInstanceFactory, cache, backend);
    cacheConfigChangeBridge.connectConfigs();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
    }
    registeredEventListeners = cache.getCacheEventNotificationService();
    backend.addListener(new CacheEventListener());
    cacheLockProvider = new TCCacheLockProvider(backend, valueModeHandler);
  }

  private static CacheConfigChangeBridge createConfigChangeBridge(ToolkitInstanceFactory toolkitInstanceFactory,
                                                                  Ehcache ehcache,
                                                                  ToolkitCacheWithMetadata<Object, Serializable> cacheWithMetadata) {
    return new CacheConfigChangeBridge(ehcache, toolkitInstanceFactory.getFullyQualifiedCacheName(ehcache),
                                       cacheWithMetadata,
                                       toolkitInstanceFactory.getOrCreateConfigChangeNotifier(ehcache));
  }

  private static void validateConfig(Ehcache ehcache) {
    CacheConfiguration cacheConfiguration = ehcache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = cacheConfiguration.getTerracottaConfiguration();

    List<ConfigError> errors = new ArrayList<ConfigError>();
    if (terracottaConfiguration == null || !terracottaConfiguration.isClustered()) {
      errors.add(new ConfigError("Cannot create clustered store for non-terracotta clustered caches"));
    }

    // TODO: move all below validation in ehcache-core?
    if (terracottaConfiguration.getValueMode() == ValueMode.IDENTITY) {
      errors.add(new ConfigError("Identity value mode is no longer supported"));
    }
    MemoryStoreEvictionPolicy policy = cacheConfiguration.getMemoryStoreEvictionPolicy();
    if (policy == MemoryStoreEvictionPolicy.FIFO || policy == MemoryStoreEvictionPolicy.LFU) {
      errors.add(new ConfigError("Policy '" + policy + "' is not a supported memory store eviction policy."));
    }

    if (cacheConfiguration.isOverflowToDisk()) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Persistence on disk on the local node is not supported with a Terracotta clustered ehcache store. Configure the Terracotta server array to be persistent instead.");
      }
    }
    boolean cachePinned = cacheConfiguration.getPinningConfiguration() != null
                          && cacheConfiguration.getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;
    if (cachePinned && cacheConfiguration.getMaxElementsOnDisk() != CacheConfiguration.DEFAULT_MAX_ELEMENTS_ON_DISK) {
      errors.add(new ConfigError("Cache pinning is not supported with maxElementsOnDisk"));
    }

    if (terracottaConfiguration.getStorageStrategy() == StorageStrategy.CLASSIC) {
      errors.add(new ConfigError("Classic storage strategy is no longer supported"));
    }

    if (errors.size() > 0) { throw new InvalidConfigurationException(errors); }
  }

  private static boolean isCheckContainsKeyOnPut(ToolkitInstanceFactory toolkitInstanceFactory, Ehcache cache) {
    ToolkitProperties toolkitProperties = toolkitInstanceFactory.getToolkit().getProperties();
    // TODO: CleanUp StorageStrategy will always be DCV2
    if (StorageStrategy.DCV2.equals(cache.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy())) {
      // use false by default
      return toolkitProperties.getBoolean(CHECK_CONTAINS_KEY_ON_PUT_PROPERTY_NAME, false);
    } else {
      // use true by default
      return toolkitProperties.getBoolean(CHECK_CONTAINS_KEY_ON_PUT_PROPERTY_NAME, true);
    }
  }

  @Override
  public void unpinAll() {
    backend.unpinAll();
  }

  @Override
  public boolean isPinned(Object key) {
    return backend.isPinned(key);
  }

  @Override
  public void setPinned(Object key, boolean pinned) {
    backend.setPinned(key, pinned);
  }

  @Override
  public void recalculateSize(Object key) {
    throw new UnsupportedOperationException("Recalculate size is not supported for Terracotta clustered caches.");
  }

  public synchronized void addStoreListener(StoreListener listener) {
    removeStoreListener(listener);
    getEventListenerList().add(StoreListener.class, listener);
  }

  public synchronized void removeStoreListener(StoreListener listener) {
    getEventListenerList().remove(StoreListener.class, listener);
  }

  private synchronized EventListenerList getEventListenerList() {
    if (listenerList == null) {
      listenerList = new EventListenerList();
      // TODO: register for event listener
    }
    return listenerList;
  }

  @Override
  public boolean put(Element element) throws CacheException {
    return putInternal(element, null);
  }

  @Override
  public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
    return putInternal(element, writerManager);
  }

  private boolean putInternal(Element element, CacheWriterManager writerManager) throws CacheException {
    if (element == null) { return true; }

    Object pKey = generatePortableKeyFor(element.getObjectKey());

    MetaData searchMetaData = createPutSearchMetaData(pKey, element);
    // Keep this before the backend put to ensure that a write behind operation can never be lost.
    // This will be handled in the Terracotta L2 as an atomic operation right after the backend put since at that
    // time a lock commit is issued that splits up the transaction. It will not be visible before either since
    // there are no other lock boundaries in the code path.
    if (writerManager != null) {
      writerManager.put(element);
    }

    return doPut(pKey, element, searchMetaData);
  }

  @Override
  public void putAll(Collection<Element> elements) throws CacheException {
    Set<EntryWithMetaData<Object, Serializable>> entries = new HashSet<EntryWithMetaData<Object, Serializable>>();
    for (Element element : elements) {
      if (!element.usesCacheDefaultLifespan()) {
        // TODO: support custom lifespan with putAll
        throw new UnsupportedOperationException("putAll() doesn't support custom lifespan");
      }
      Object pKey = generatePortableKeyFor(element.getObjectKey());
      ElementData elementData = valueModeHandler.createElementData(element);
      MetaData metaData = createPutSearchMetaData(pKey, element);
      entries.add(backend.createEntryWithMetaData(pKey, elementData, metaData));
    }
    backend.putAllWithMetaData(entries);
  }

  @Override
  public Element get(Object key) {
    Object pKey = generatePortableKeyFor(key);
    Serializable value = backend.get(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public Element getQuiet(Object key) {
    Object pKey = generatePortableKeyFor(key);
    Serializable value = backend.getQuiet(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public List getKeys() {
    return Collections.unmodifiableList(Arrays.asList(new RealObjectKeySet(this.valueModeHandler, backend.keySet())));
  }

  @Override
  public Element remove(Object key) {
    return removeWithWriter(key, null);
  }

  @Override
  public void removeAll(Collection<?> keys) {
    Set<EntryWithMetaData<Object, Serializable>> entries = new HashSet<EntryWithMetaData<Object, Serializable>>();
    for (Object key : keys) {
      Object pKey = generatePortableKeyFor(key);
      MetaData metaData = createRemoveSearchMetaData(pKey);
      entries.add(backend.createEntryWithMetaData(pKey, null, metaData));
    }
    backend.removeAllWithMetaData(entries);
  }

  @Override
  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
    if (key == null) { return null; }

    // Keep this before the backend remove to ensure that a write behind operation can never be lost.
    // This will be handled in the Terracotta L2 as an atomic operation right after the backend remove since at that
    // time a lock commit is issued that splits up the transaction. It will not be visible before either since there
    // are no other lock boundaries in the code path.
    if (writerManager != null) {
      writerManager.remove(new CacheEntry(key, get(key)));
    }

    Object pKey = generatePortableKeyFor(key);
    Serializable value = backend.removeWithMetaData(pKey, createRemoveSearchMetaData(pKey));
    Element element = this.valueModeHandler.createElement(key, value);
    if (keyLookupCache != null) {
      keyLookupCache.remove(key);
    }

    if (element != null) {
      return element;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug(cache.getName() + " Cache: Cannot remove entry as key " + key + " was not found");
      }
      return null;
    }

  }

  @Override
  public void removeAll() throws CacheException {
    backend.clearWithMetaData(createClearSearchMetaData());
    keyLookupCache.clear();
  }

  @Override
  public Element putIfAbsent(Element element) throws NullPointerException {
    Object pKey = generatePortableKeyFor(element.getObjectKey());
    MetaData searchMetaData = createPutSearchMetaData(pKey, element);
    ElementData value = valueModeHandler.createElementData(element);
    Serializable data = backend.putIfAbsentWithMetaData(pKey, value, searchMetaData);
    return data == null ? null : this.valueModeHandler.createElement(element.getKey(), data);
  }

  @Override
  public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
    Object pKey = generatePortableKeyFor(element.getKey());
    ToolkitLock lock = backend.createFinegrainedLock(pKey);
    lock.lock();
    try {
      Element oldElement = getQuiet(element.getKey());
      if (comparator.equals(oldElement, element)) { return remove(element.getKey()); }
    } finally {
      lock.unlock();
    }
    return null;
  }

  @Override
  public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
      IllegalArgumentException {
    Object pKey = generatePortableKeyFor(element.getKey());
    ToolkitLock lock = backend.createFinegrainedLock(pKey);
    lock.lock();
    try {
      Element oldElement = getQuiet(element.getKey());
      if (comparator.equals(oldElement, element)) { return putInternal(oldElement, null); }
    } finally {
      lock.unlock();
    }
    return false;
  }

  @Override
  public Element replace(Element element) throws NullPointerException {
    Object pKey = generatePortableKeyFor(element.getKey());
    ToolkitLock lock = backend.createFinegrainedLock(pKey);
    lock.lock();
    try {
      Element oldElement = getQuiet(element.getKey());
      if (oldElement != null) {
        putInternal(element, null);
      }
      return oldElement;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void dispose() {
    // TODO: backend.destroyLocally()
    cacheConfigChangeBridge.disconnectConfigs();
  }

  @Override
  public int getSize() {
    return getTerracottaClusteredSize();
  }

  @Override
  public int getInMemorySize() {
    return (int) backend.localOnHeapSizeInBytes();
  }

  @Override
  public int getOffHeapSize() {
    return backend.localOffHeapSize();
  }

  @Override
  public int getOnDiskSize() {
    return 0;
  }

  @Override
  public int getTerracottaClusteredSize() {
    return backend.size();
  }

  @Override
  public long getInMemorySizeInBytes() {
    return backend.localOnHeapSizeInBytes();
  }

  @Override
  public long getOffHeapSizeInBytes() {
    return backend.localOffHeapSizeInBytes();
  }

  @Override
  public long getOnDiskSizeInBytes() {
    return 0;
  }

  @Override
  public boolean hasAbortedSizeOf() {
    return false;
  }

  @Override
  public Status getStatus() {
    return Status.STATUS_ALIVE;
  }

  @Override
  public boolean containsKey(Object key) {
    Object pKey = generatePortableKeyFor(key);
    return backend.containsKey(pKey);
  }

  @Override
  public boolean containsKeyOnDisk(Object key) {
    return false;
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    Object pKey = generatePortableKeyFor(key);
    return backend.containsKeyLocalOffHeap(pKey);
  }

  @Override
  public boolean containsKeyInMemory(Object key) {
    Object pKey = generatePortableKeyFor(key);
    return backend.containsKeyLocalOnHeap(pKey);
  }

  /**
   * Expire all elements.
   * <p/>
   * This is a default implementation which does nothing. Expiration on demand is only implemented for disk stores.
   */
  @Override
  public void expireElements() {
    // empty implementation
  }

  @Override
  public void flush() {
    // should be emptied if clearOnFlush is true
    if (cache.getCacheConfiguration().isClearOnFlush()) {
      backend.clear();
      if (keyLookupCache != null) {
        keyLookupCache.clear();
      }
    }
  }

  @Override
  public boolean bufferFull() {
    return false;
  }

  @Override
  public Policy getInMemoryEvictionPolicy() {
    return null;
  }

  @Override
  public void setInMemoryEvictionPolicy(Policy policy) {
    //
  }

  @Override
  public Object getInternalContext() {
    return cacheLockProvider;
  }

  @Override
  public boolean isCacheCoherent() {
    return isClusterCoherent();
  }

  @Override
  public boolean isClusterCoherent() throws TerracottaNotRunningException {
    return !backend.isBulkLoadEnabledInCluster();
  }

  @Override
  public boolean isNodeCoherent() throws TerracottaNotRunningException {
    return !backend.isBulkLoadEnabledInCurrentNode();
  }

  @Override
  public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
    backend.setBulkLoadEnabledInCurrentNode(!coherent);
  }

  @Override
  public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException,
      InterruptedException {
    backend.waitUntilBulkLoadCompleteInCluster();
  }

  @Override
  public Object getMBean() {
    return null;
  }

  @Override
  public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
    if (!extractors.isEmpty()) { throw new CacheException("Search attributes only supported in enterprise edition"); }
  }

  @Override
  public Results executeQuery(StoreQuery query) throws SearchException {
    throw new UnsupportedOperationException("Search execution unsupported in non-enterprise edition");
  }

  @Override
  public <T> Attribute<T> getSearchAttribute(String attributeName) {
    return null;
  }

  @Override
  public Map<Object, Element> getAllQuiet(Collection<?> keys) {
    return doGetAll(keys, true);
  }

  @Override
  public Map<Object, Element> getAll(Collection<?> keys) {
    return doGetAll(keys, false);
  }

  private Map<Object, Element> doGetAll(Collection<?> keys, boolean quiet) {
    List pKeys = new ArrayList(keys.size());
    for (Object key : keys) {
      pKeys.add(generatePortableKeyFor(key));
    }
    final Map<Object, Serializable> values;
    if (quiet) {
      values = backend.getAllQuiet(pKeys);
    } else {
      values = backend.getAll(pKeys);
    }
    Map<Object, Element> elements = new HashMap();
    Set<Entry<Object, Serializable>> entrySet = values.entrySet();
    for (Map.Entry<Object, Serializable> entry : entrySet) {
      Object key = this.valueModeHandler.getRealKeyObject(entry.getKey());
      elements.put(key, this.valueModeHandler.createElement(key, entry.getValue()));
    }
    return elements;
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

  protected MetaData createPutSearchMetaData(Object portableKey, Element element) {
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

  private boolean doPut(Object portableKey, Element element, MetaData searchMetaData) {

    ElementData value = valueModeHandler.createElementData(element);
    // TODO: make sure creation time is in secs
    int creationTimeInSecs = (int) element.getCreationTime();
    int customTTI = element.getTimeToIdle();
    int customTTL = element.getTimeToLive();

    if (checkContainsKeyOnPut) {
      return backend.putWithMetaData(portableKey, value, creationTimeInSecs, customTTI, customTTL, searchMetaData) == null ? true
          : false;
    } else {
      backend.putNoReturnWithMetaData(portableKey, value, creationTimeInSecs, customTTI, customTTL, searchMetaData);
      return true;
    }
  }

  @Override
  public Element unsafeGet(Object key) {
    Object pKey = generatePortableKeyFor(key);
    Serializable value = backend.unsafeGet(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public Set getLocalKeys() {
    return new RealObjectKeySet(valueModeHandler, backend.localKeySet());
  }

  @Override
  public TransactionalMode getTransactionalMode() {
    return transactionalMode;
  }

  public boolean isSearchable() {
    return false;
  }

  private class CacheEventListener implements ToolkitCacheListener {

    @Override
    public void onEviction(Object key) {
      Element element = new Element(valueModeHandler.getRealKeyObject(key), null);
      registeredEventListeners.notifyElementEvicted(element, false);
    }

    @Override
    public void onExpiration(Object key) {
      Element element = new Element(valueModeHandler.getRealKeyObject(key), null);
      registeredEventListeners.notifyElementExpiry(element, false);
    }

  }

}
