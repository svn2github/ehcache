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
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
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
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitProperties;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.ToolkitMapConfigFields;
import org.terracotta.toolkit.config.ToolkitMapConfigFields.PinningStore;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;
import org.terracotta.toolkit.internal.meta.MetaData;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.event.EventListenerList;

public class ClusteredStore implements TerracottaStore {

  private static final Logger                        LOG                       = LoggerFactory
                                                                                   .getLogger(ClusteredStore.class
                                                                                       .getName());
  private static final String                        DELIM                     = "|";
  private static final String                        ROOT_NAME_STORE           = "ehcache-store";

  private final ToolkitProperties                    toolkitProperties;
  private static final String                        CHECK_CONTAINS_KEY_ON_PUT = "ehcache.clusteredStore.checkContainsKeyOnPut";
  private volatile boolean                           checkContainsKeyOnPut;

  private final ToolkitCacheWithMetadata             backend;
  protected final ValueModeHandler                   valueModeHandler;
  private final int                                  localKeyCacheMaxsize;
  private final CacheConfiguration.TransactionalMode transactionalMode;

  /**
   * The cache this store is associated with.
   */
  private final Ehcache                              cache;
  private final Map                                  keyLookupCache;
  private final Set<CacheConfiguration>              linkedConfigurations      = new CopyOnWriteArraySet<CacheConfiguration>();
  private EventListenerList                          listenerList;

  public ClusteredStore(Toolkit toolkit, Ehcache cache) {
    this.cache = cache;

    toolkitProperties = toolkit.getProperties();
    final CacheConfiguration ehcacheConfig = cache.getCacheConfiguration();
    checkMemoryStoreEvictionPolicy(ehcacheConfig);
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();

    if (terracottaConfiguration == null || !terracottaConfiguration.isClustered()) { throw new IllegalArgumentException(
                                                                                                                        "Cannot create clustered store for non-terracotta clustered caches"); }
    transactionalMode = ehcacheConfig.getTransactionalMode();

    if (ehcacheConfig.isOverflowToDisk()) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Persistence on disk on the local node is not supported with a Terracotta clustered ehcache store. Configure the Terracotta server array to be persistent instead.");
      }
    }
    valueModeHandler = ValueModeHandlerFactory.createValueModeHandler(this, ehcacheConfig);

    if (terracottaConfiguration.getLocalKeyCache()) {
      localKeyCacheMaxsize = terracottaConfiguration.getLocalKeyCacheSize();
      keyLookupCache = new ConcurrentHashMap<Object, Object>();
    } else {
      localKeyCacheMaxsize = -1;
      keyLookupCache = null;
    }
    // TODO: CleanUp StorageStrategy will always be DCV2
    if (StorageStrategy.DCV2.equals(cache.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy())) {
      // use false by default
      checkContainsKeyOnPut = toolkitProperties.getBoolean(CHECK_CONTAINS_KEY_ON_PUT, false);
    } else {
      // use true by default
      checkContainsKeyOnPut = toolkitProperties.getBoolean(CHECK_CONTAINS_KEY_ON_PUT, true);
    }

    final Configuration clusteredCacheConfig = createClusteredMapConfig(toolkit.getConfigBuilderFactory()
        .newToolkitCacheConfigBuilder(), cache);
    backend = (ToolkitCacheWithMetadata) toolkit.getCache(storeRootName(cache.getCacheManager().getName(),
                                                                        cache.getName()), clusteredCacheConfig);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
    }
  }

  private static String storeRootName(final String cacheMgrName, final String cacheName) {
    return ROOT_NAME_STORE + DELIM + cacheMgrName + DELIM + cacheName;
  }

  private Configuration createClusteredMapConfig(ToolkitCacheConfigBuilder builder, Ehcache ehcache) {
    final CacheConfiguration ehcacheConfig = ehcache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    builder.maxTTISeconds((int) ehcacheConfig.getTimeToIdleSeconds());
    builder.maxTTLSeconds((int) ehcacheConfig.getTimeToLiveSeconds());
    boolean cachePinned = cache.getCacheConfiguration().getPinningConfiguration() != null
                          && cache.getCacheConfiguration().getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;
    builder.maxTotalCount(cachePinned ? 0 : ehcacheConfig.getMaxElementsOnDisk());
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

    final String cmName = ehcache.getCacheManager().isNamed() ? ehcache.getCacheManager().getName()
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

  private int calculateCorrectConcurrency(CacheConfiguration cacheConfiguration) {
    int maxElementOnDisk = cacheConfiguration.getMaxElementsOnDisk();
    if (maxElementOnDisk <= 0 || maxElementOnDisk >= ToolkitMapConfigFields.DEFAULT_CONCURRENCY) { return ToolkitMapConfigFields.DEFAULT_CONCURRENCY; }
    int concurrency = 1;
    while (concurrency * 2 <= maxElementOnDisk) {// this while loop is not very time consuming, maximum it will do 8
                                                 // iterations
      concurrency *= 2;
    }
    return concurrency;
  }

  private void checkMemoryStoreEvictionPolicy(final CacheConfiguration config) {
    MemoryStoreEvictionPolicy policy = config.getMemoryStoreEvictionPolicy();
    if (policy == MemoryStoreEvictionPolicy.FIFO || policy == MemoryStoreEvictionPolicy.LFU) { throw new IllegalArgumentException(
                                                                                                                                  "Policy '"
                                                                                                                                      + policy
                                                                                                                                      + "' is not a supported memory store eviction policy."); }
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
    Object value = valueModeHandler.createElementData(element);

    return doPut(pKey, value, searchMetaData);
  }

  @Override
  public void putAll(Collection<Element> elements) throws CacheException {
    Map data = new HashMap();
    for (Element element : elements) {
      Object pKey = generatePortableKeyFor(element.getObjectKey());
      data.put(pKey, valueModeHandler.createElementData(element));
    }
    // TODO: Create search data and put that also. for that add a putAllWithMetaData API also
    backend.putAll(data);

  }

  @Override
  public Element get(Object key) {
    Object pKey = generatePortableKeyFor(key);
    Object value = backend.get(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public Element getQuiet(Object key) {
    Object pKey = generatePortableKeyFor(key);
    Object value = backend.getQuiet(pKey);
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

    // TODO: Add removeAllWithMeta data to toolkit cache.
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
    Object value = backend.removeWithMetaData(pKey, createRemoveSearchMetaData(pKey));
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

    Object value = valueModeHandler.createElementData(element);

    Object data = backend.putIfAbsentWithMetaData(pKey, (Serializable) value, searchMetaData);

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
    //

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
  public void flush() throws IOException {
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

    return null;
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
    final Map values;
    if (quiet) {
      values = backend.getAllQuiet(pKeys);
    } else {
      values = backend.getAll(pKeys);
    }
    Map<Object, Element> elements = new HashMap();
    Set<Map.Entry> entrySet = values.entrySet();
    for (Map.Entry object : entrySet) {
      Object key = this.valueModeHandler.getRealKeyObject(object.getKey());
      elements.put(key, this.valueModeHandler.createElement(key, object.getValue()));
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

  private boolean doPut(Object portableKey, Object value, MetaData searchMetaData) {
    if (checkContainsKeyOnPut) {
      return backend.putWithMetaData(portableKey, (Serializable) value, searchMetaData) == null ? true : false;
    } else {
      backend.putNoReturnWithMetaData(portableKey, (Serializable) value, searchMetaData);
      return true;
    }
  }

  @Override
  public Element unsafeGet(Object key) {
    Object pKey = generatePortableKeyFor(key);
    Object value = backend.unsafeGet(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public Element unsafeGetQuiet(Object key) {
    Object pKey = generatePortableKeyFor(key);
    // TODO: Add unsafeGetQuiet to toolkit
    Object value = backend.unsafeGet(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public Element unlockedGet(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Element unlockedGetQuiet(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set getLocalKeys() {
    return new RealObjectKeySet(valueModeHandler, backend.localKeySet());
  }

  @Override
  public TransactionalMode getTransactionalMode() {
    return transactionalMode;
  }

}
