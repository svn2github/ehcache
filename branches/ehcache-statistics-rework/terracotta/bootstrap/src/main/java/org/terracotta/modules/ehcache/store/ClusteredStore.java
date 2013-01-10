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
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.ConfigError;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
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
import net.sf.ehcache.util.SetAsList;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ClusteredCacheInternalContext;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.concurrency.TCCacheLockProvider;
import org.terracotta.modules.ehcache.store.bulkload.BulkLoadShutdownHook;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;
import org.terracotta.statistics.observer.OperationObserver;
import org.terracotta.statistics.Statistic;
import javax.swing.event.EventListenerList;

import static net.sf.ehcache.statistics.StatisticBuilder.operation;

public class ClusteredStore implements TerracottaStore, StoreListener {

  private static final Logger                                 LOG                                     = LoggerFactory
                                                                                                          .getLogger(ClusteredStore.class
                                                                                                              .getName());
  private static final String                                 CHECK_CONTAINS_KEY_ON_PUT_PROPERTY_NAME = "ehcache.clusteredStore.checkContainsKeyOnPut";
  private static final String                                 TRANSACTIONAL_MODE                      = "trasactionalMode";

  // final protected fields
  protected final ClusteredStoreBackend<String, Serializable> backend;
  protected final ValueModeHandler                            valueModeHandler;
  protected final ToolkitInstanceFactory                      toolkitInstanceFactory;
  protected final Ehcache                                     cache;
  protected final String                                      fullyQualifiedCacheName;

  // final private fields
  private final boolean                                       checkContainsKeyOnPut;
  private final int                                           localKeyCacheMaxsize;
  private final CacheConfiguration.TransactionalMode          transactionalMode;
  private final Map<Object, String>                           keyLookupCache;
  private final CacheConfigChangeBridge                       cacheConfigChangeBridge;
  private final RegisteredEventListeners                      registeredEventListeners;
  private final ClusteredCacheInternalContext                 internalContext;
  private final CacheEventListener                            evictionListener;

  // non-final private fields
  private EventListenerList                                   listenerList;

  private final OperationObserver<EvictionOutcome>            evictionObserver = operation(EvictionOutcome.class).named("eviction").of(this).build();

  public ClusteredStore(ToolkitInstanceFactory toolkitInstanceFactory, Ehcache cache,
                        BulkLoadShutdownHook bulkLoadShutdownHook) {
    validateConfig(cache);

    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.cache = cache;
    this.fullyQualifiedCacheName = toolkitInstanceFactory.getFullyQualifiedCacheName(cache);
    final CacheConfiguration ehcacheConfig = cache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();

    ConcurrentMap<String, Serializable> configMap = toolkitInstanceFactory.getOrCreateClusteredStoreConfigMap(cache
        .getCacheManager().getName(), cache.getName());
    CacheConfiguration.TransactionalMode transactionalModeTemp = (TransactionalMode) configMap.get(TRANSACTIONAL_MODE);
    if (transactionalModeTemp == null) {
      configMap.putIfAbsent(TRANSACTIONAL_MODE, ehcacheConfig.getTransactionalMode());
      transactionalModeTemp = (TransactionalMode) configMap.get(TRANSACTIONAL_MODE);
    }
    transactionalMode = transactionalModeTemp;

    valueModeHandler = ValueModeHandlerFactory.createValueModeHandler(this, ehcacheConfig,
                                                                      toolkitInstanceFactory.getToolkitSerializer());

    if (terracottaConfiguration.getLocalKeyCache()) {
      localKeyCacheMaxsize = terracottaConfiguration.getLocalKeyCacheSize();
      keyLookupCache = new ConcurrentHashMap<Object, String>();
    } else {
      localKeyCacheMaxsize = -1;
      keyLookupCache = null;
    }

    ToolkitInternal toolkitInternal = (ToolkitInternal) toolkitInstanceFactory.getToolkit();
    checkContainsKeyOnPut = toolkitInternal.getProperties().getBoolean(CHECK_CONTAINS_KEY_ON_PUT_PROPERTY_NAME);
    backend = new ClusteredStoreBackend<String, Serializable>(toolkitInternal,
                                                              toolkitInstanceFactory.getOrCreateToolkitCache(cache),
                                                              bulkLoadShutdownHook, this);
    LOG.info(getConcurrencyValueLogMsg(cache.getName(),
                                       backend.getConfiguration()
                                           .getInt(ToolkitStoreConfigFields.CONCURRENCY_FIELD_NAME)));
    // connect configurations
    cacheConfigChangeBridge = createConfigChangeBridge(toolkitInstanceFactory, cache, backend);
    cacheConfigChangeBridge.connectConfigs();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
    }
    registeredEventListeners = cache.getCacheEventNotificationService();
    evictionListener = new CacheEventListener();
    backend.addListener(evictionListener);
    CacheLockProvider cacheLockProvider = new TCCacheLockProvider(backend, valueModeHandler);
    internalContext = new ClusteredCacheInternalContext(toolkitInstanceFactory.getToolkit(), cacheLockProvider);
    bulkLoadShutdownHook.init();
  }

  public String getFullyQualifiedCacheName() {
    return fullyQualifiedCacheName;
  }

  private static CacheConfigChangeBridge createConfigChangeBridge(ToolkitInstanceFactory toolkitInstanceFactory,
                                                                  Ehcache ehcache,
                                                                  ToolkitCacheInternal<String, Serializable> cache) {
    return new CacheConfigChangeBridge(ehcache, toolkitInstanceFactory.getFullyQualifiedCacheName(ehcache), cache,
                                       toolkitInstanceFactory.getOrCreateConfigChangeNotifier(ehcache));
  }

  private static void validateConfig(Ehcache ehcache) {
    CacheConfiguration cacheConfiguration = ehcache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = cacheConfiguration.getTerracottaConfiguration();

    List<ConfigError> errors = new ArrayList<ConfigError>();
    if (terracottaConfiguration == null || !terracottaConfiguration.isClustered()) { throw new InvalidConfigurationException(
                                                                                                                             "Cannot create clustered store for non-terracotta clustered caches"); }

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
    if (cachePinned && cacheConfiguration.getMaxEntriesInCache() != CacheConfiguration.DEFAULT_MAX_ENTRIES_IN_CACHE) {
      errors.add(new ConfigError("Cache pinning is not supported with maxEntriesInCache"));
    }

    if (errors.size() > 0) { throw new InvalidConfigurationException(errors); }
  }

  @Override
  public void unpinAll() {
    backend.unpinAll();
  }

  @Override
  public boolean isPinned(Object key) {
    String pKey = generatePortableKeyFor(key);
    return backend.isPinned(pKey);
  }

  @Override
  public void setPinned(Object key, boolean pinned) {
    String pKey = generatePortableKeyFor(key);
    backend.setPinned(pKey, pinned);
  }

  @Override
  public void recalculateSize(Object key) {
    throw new UnsupportedOperationException("Recalculate size is not supported for Terracotta clustered caches.");
  }

  @Override
  public synchronized void addStoreListener(StoreListener listener) {
    removeStoreListener(listener);
    getEventListenerList().add(StoreListener.class, listener);
  }

  @Override
  public synchronized void removeStoreListener(StoreListener listener) {
    getEventListenerList().remove(StoreListener.class, listener);
  }

  private synchronized EventListenerList getEventListenerList() {
    if (listenerList == null) {
      listenerList = new EventListenerList();
      // TODO: do we still need to support sending notifications when bulk-load turns on/off
    }
    return listenerList;
  }

  @Override
  public void clusterCoherent(final boolean clusterCoherent) {
    Object[] listeners = getEventListenerList().getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == StoreListener.class) {
        ((StoreListener) listeners[i + 1]).clusterCoherent(clusterCoherent);
      }
    }
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

    String pKey = generatePortableKeyFor(element.getObjectKey());
    // extractSearchAttributes(element);

    // Keep this before the backend put to ensure that a write behind operation can never be lost.
    // This will be handled in the Terracotta L2 as an atomic operation right after the backend put since at that
    // time a lock commit is issued that splits up the transaction. It will not be visible before either since
    // there are no other lock boundaries in the code path.
    if (writerManager != null) {
      writerManager.put(element);
    }
    if (element.usesCacheDefaultLifespan()) {
      return doPut(pKey, element);
    } else {
      return doPutWithCustomLifespan(pKey, element);
    }
  }

  @Override
  public void putAll(Collection<Element> elements) throws CacheException {
    Map<String, Serializable> entries = new HashMap<String, Serializable>();
    for (Element element : elements) {
      if (!element.usesCacheDefaultLifespan()) {
        // TODO: support custom lifespan with putAll
        throw new UnsupportedOperationException("putAll() doesn't support custom lifespan");
      }
      String pKey = generatePortableKeyFor(element.getObjectKey());
      // extractSearchAttributes(element);
      ElementData elementData = valueModeHandler.createElementData(element);
      entries.put(pKey, elementData);
    }
    backend.putAll(entries);
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
    String pKey = generatePortableKeyFor(key);
    Serializable value = backend.getQuiet(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public List getKeys() {
    return Collections.unmodifiableList(new SetAsList(new RealObjectKeySet(this.valueModeHandler, backend.keySet(),
                                                                           false)));
  }

  @Override
  public Element remove(Object key) {
    return removeWithWriter(key, null);
  }

  @Override
  public void removeAll(Collection<?> keys) {
    Set<String> entries = new HashSet<String>();
    for (Object key : keys) {
      String pKey = generatePortableKeyFor(key);
      entries.add(pKey);
    }
    backend.removeAll(entries);
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

    String pKey = generatePortableKeyFor(key);
    Serializable value = backend.remove(pKey);
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
    backend.clear();
    if (keyLookupCache != null) {
      keyLookupCache.clear();
    }
  }

  @Override
  public Element putIfAbsent(Element element) throws NullPointerException {
    String pKey = generatePortableKeyFor(element.getObjectKey());
    // extractSearchAttributes(element);
    ElementData value = valueModeHandler.createElementData(element);
    Serializable data = backend.putIfAbsent(pKey, value);
    return data == null ? null : this.valueModeHandler.createElement(element.getKey(), data);
  }

  @Override
  public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
    String pKey = generatePortableKeyFor(element.getKey());
    ToolkitReadWriteLock lock = backend.createLockForKey(pKey);
    lock.writeLock().lock();
    try {
      Element oldElement = getQuiet(element.getKey());
      if (comparator.equals(oldElement, element)) { return remove(element.getKey()); }
    } finally {
      lock.writeLock().unlock();
    }
    return null;
  }

  @Override
  public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
      IllegalArgumentException {
    String pKey = generatePortableKeyFor(element.getKey());
    ToolkitReadWriteLock lock = backend.createLockForKey(pKey);
    lock.writeLock().lock();
    try {
      Element oldElement = getQuiet(element.getKey());
      if (comparator.equals(oldElement, old)) { return putInternal(element, null); }
    } finally {
      lock.writeLock().unlock();
    }
    return false;
  }

  @Override
  public Element replace(Element element) throws NullPointerException {
    // TODO: Revisit
    String pKey = generatePortableKeyFor(element.getKey());
    ToolkitReadWriteLock lock = backend.createLockForKey(pKey);
    lock.writeLock().lock();
    try {
      Element oldElement = getQuiet(element.getKey());
      if (oldElement != null) {
        putInternal(element, null);
      }
      return oldElement;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void dispose() {
    backend.removeListener(evictionListener);
    backend.disposeLocally();
    cacheConfigChangeBridge.disconnectConfigs();
    toolkitInstanceFactory.removeNonStopConfigforCache(cache);
  }

  @Override
  public int getSize() {
    return getTerracottaClusteredSize();
  }

  @Override
  @Statistic(name="size", tags="local-heap")
  public int getInMemorySize() {
    return backend.localOnHeapSize();
  }

  @Override
  @Statistic(name="size", tags="local-offheap")
  public int getOffHeapSize() {
    return backend.localOffHeapSize();
  }

  @Override
  public int getOnDiskSize() {
    return 0;
  }

  @Override
  @Statistic(name="size", tags="remote")
  public int getTerracottaClusteredSize() {
    return backend.size();
  }

  @Override
  @Statistic(name="size-in-bytes", tags="local-heap")
  public long getInMemorySizeInBytes() {
    return backend.localOnHeapSizeInBytes();
  }

  @Override
  @Statistic(name="size-in-bytes", tags="local-offheap")
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
    String pKey = generatePortableKeyFor(key);
    return backend.containsKeyLocalOffHeap(pKey);
  }

  @Override
  public boolean containsKeyInMemory(Object key) {
    String pKey = generatePortableKeyFor(key);
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
    // memory store eviction policy not configurable for clustered stores
    throw new UnsupportedOperationException();
  }

  @Override
  public void setInMemoryEvictionPolicy(Policy policy) {
    // memory store eviction policy not configurable for clustered stores
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getInternalContext() {
    return internalContext;
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
    List<String> pKeys = new ArrayList(keys.size());
    for (Object key : keys) {
      pKeys.add(generatePortableKeyFor(key));
    }
    final Map<String, Serializable> values;
    if (quiet) {
      values = backend.getAllQuiet(pKeys);
    } else {
      values = backend.getAll(pKeys);
    }
    Map<Object, Element> elements = new HashMap();
    Set<Entry<String, Serializable>> entrySet = values.entrySet();
    for (Map.Entry<String, Serializable> entry : entrySet) {
      Object key = this.valueModeHandler.getRealKeyObject(entry.getKey(), false);
      elements.put(key, this.valueModeHandler.createElement(key, entry.getValue()));
    }
    return elements;
  }

  /**
   * Generates a portable key for the supplied object.
   */
  public String generatePortableKeyFor(final Object obj) {
    boolean useCache = shouldUseCache(obj);

    if (useCache) {
      String value = keyLookupCache.get(obj);
      if (value != null) { return value; }
    }

    String key;
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

  private boolean doPut(String portableKey, Element element) {

    ElementData value = valueModeHandler.createElementData(element);
    if (checkContainsKeyOnPut) {
      return backend.put(portableKey, value) == null ? true : false;
    } else {
      backend.putNoReturn(portableKey, value);
      return true;
    }
  }

  private boolean doPutWithCustomLifespan(String portableKey, Element element) {

    ElementData value = valueModeHandler.createElementData(element);
    int creationTimeInSecs = (int) (element.getCreationTime() / 1000);
    int customTTI = element.isEternal() ? Integer.MAX_VALUE : element.getTimeToIdle();
    int customTTL = element.isEternal() ? Integer.MAX_VALUE : element.getTimeToLive();
    if (checkContainsKeyOnPut) {
      return backend.put(portableKey, value, creationTimeInSecs, customTTI, customTTL) == null ? true : false;
    } else {
      backend.putNoReturn(portableKey, value, creationTimeInSecs, customTTI, customTTL);
      return true;
    }
  }

  @Override
  public Element unsafeGet(Object key) {
    String pKey = generatePortableKeyFor(key);
    Serializable value = backend.unsafeLocalGet(pKey);
    if (value == null) { return null; }
    Element element = this.valueModeHandler.createElement(key, value);
    return element;
  }

  @Override
  public Set getLocalKeys() {
    return Collections.unmodifiableSet(new RealObjectKeySet(valueModeHandler, backend.localKeySet(), true));
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
      evictionObserver.begin();
      evictionObserver.end(EvictionOutcome.SUCCESS);
      Element element = new Element(valueModeHandler.getRealKeyObject((String) key, false), null);
      registeredEventListeners.notifyElementEvicted(element, false);
    }

    @Override
    public void onExpiration(Object key) {
      Element element = new Element(valueModeHandler.getRealKeyObject((String) key, false), null);
      registeredEventListeners.notifyElementExpiry(element, false);
    }

  }

  // tests assert on the log msg printed
  private static String getConcurrencyValueLogMsg(String name, int concurrency) {
    return "Cache [" + name + "] using concurrency: " + concurrency;
  }

  @Override
  public WriteBehind createWriteBehind() {
    throw new UnsupportedOperationException();
  }
}
