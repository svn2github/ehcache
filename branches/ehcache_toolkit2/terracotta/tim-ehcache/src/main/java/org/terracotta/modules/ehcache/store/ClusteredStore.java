/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitCache;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.config.ToolkitCacheConfigFields;
import org.terracotta.toolkit.config.ToolkitMapConfigFields.PinningStore;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClusteredStore implements Store {
  private static final Logger                        LOG = LoggerFactory.getLogger(ClusteredStore.class.getName());
  private final String                               qualifiedCacheName;
  private final Consistency                          initialCoherenceMode;
  private final CacheConfiguration.TransactionalMode transactionalMode;
  private final boolean                              localCacheEnabled;
  private final boolean                              cachePinned;
  private final int                                  localKeyCacheMaxsize;
  private final Map<Object, Object>                  keyLookupCache;
  private final ClusteredStoreBackendImpl            backend;
  protected transient Ehcache                        cache;

  public ClusteredStore(Toolkit toolkit, Ehcache cache, long uniqueId) {
    // appending a unique identifier to the cache name is necessary to avoid collisions when the cache name starts with
    // "_" or if the name contains characters not legal for use as a filesystem path (ie. L2 lucene index path)
    this.qualifiedCacheName = cache.getCacheManager().getName() + "_" + cache.getName() + "_" + uniqueId;

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

    cachePinned = cache.getCacheConfiguration().getPinningConfiguration() != null
                  && cache.getCacheConfiguration().getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;

    if (terracottaConfiguration.getLocalKeyCache()) {
      localKeyCacheMaxsize = terracottaConfiguration.getLocalKeyCacheSize();
      keyLookupCache = new ConcurrentHashMap<Object, Object>();
    } else {
      localKeyCacheMaxsize = -1;
      keyLookupCache = null;
    }
    final Configuration clusteredCacheConfig = createClusteredMapConfig(toolkit.getConfigBuilderFactory()
        .newToolkitCacheConfigBuilder(), cache);
    ToolkitCache clusteredCache = toolkit.getCache(qualifiedCacheName, clusteredCacheConfig);
    this.backend = new ClusteredStoreBackendImpl(clusteredCache);
    // this.backend = new ClusteredStoreBackendImpl(clusteredMapConfig, map, valueModeHandler,
    // cache.getCacheEventNotificationService(), qualifiedCacheName, this);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
    }

  }

  private Configuration createClusteredMapConfig(ToolkitCacheConfigBuilder builder, Ehcache ehcache) {
    final CacheConfiguration ehcacheConfig = ehcache.getCacheConfiguration();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    builder.maxTTISeconds((int) ehcacheConfig.getTimeToIdleSeconds());
    builder.maxTTLSeconds((int) ehcacheConfig.getTimeToLiveSeconds());
    // builder.name(qualifiedCacheName);
    builder.maxTotalCount(cachePinned ? 0 : ehcacheConfig.getMaxElementsOnDisk());
    builder.localCacheEnabled(localCacheEnabled);

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

    if (ehcache.getCacheManager().isNamed()) {
      builder.localCacheEnabled(true);
      builder.localStoreManagerName(ehcache.getCacheManager().getName());
    }
    // builder.localStoreName(ehcache.getName());
    // builder.localStoreUUID(ehcache.getGuid());

    builder.pinningStore(getPinningStoreForConfiguration(ehcacheConfig));

    if (ehcacheConfig.getMaxEntriesLocalHeap() > 0) {
      builder.maxCountLocalHeap((int) ehcacheConfig.getMaxEntriesLocalHeap());
    }

    if (ehcacheConfig.getMaxBytesLocalHeap() > 0) {
      builder.maxBytesLocalHeap(ehcacheConfig.getMaxBytesLocalHeap());
    }

    if (ehcacheConfig.getMaxBytesLocalOffHeap() > 0) {
      builder.maxBytesLocalOffheap(ehcacheConfig.getMaxBytesLocalOffHeap());
    }

    builder.offheapEnabled(ehcacheConfig.isOverflowToOffHeap());

    return builder.build();
  }

  private PinningStore getPinningStoreForConfiguration(CacheConfiguration ehcacheConfig) {
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
    if (maxElementOnDisk <= 0 || maxElementOnDisk >= 256) { return ToolkitCacheConfigFields.DEFAULT_CONCURRENCY; }
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
    //

  }

  @Override
  public boolean isPinned(Object key) {

    return false;
  }

  @Override
  public void setPinned(Object key, boolean pinned) {
    //

  }

  @Override
  public void addStoreListener(StoreListener listener) {

    //
  }

  @Override
  public void removeStoreListener(StoreListener listener) {

    //
  }

  @Override
  public boolean put(Element element) throws CacheException {

    return false;
  }

  @Override
  public void putAll(Collection<Element> elements) throws CacheException {

    //
  }

  @Override
  public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {

    return false;
  }

  @Override
  public Element get(Object key) {

    return null;
  }

  @Override
  public Element getQuiet(Object key) {

    return null;
  }

  @Override
  public List getKeys() {

    return null;
  }

  @Override
  public Element remove(Object key) {

    return null;
  }

  @Override
  public void removeAll(Collection<?> keys) {

    //
  }

  @Override
  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {

    return null;
  }

  @Override
  public void removeAll() throws CacheException {
    //

  }

  @Override
  public Element putIfAbsent(Element element) throws NullPointerException {

    return null;
  }

  @Override
  public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {

    return null;
  }

  @Override
  public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
      IllegalArgumentException {

    return false;
  }

  @Override
  public Element replace(Element element) throws NullPointerException {

    return null;
  }

  @Override
  public void dispose() {
    //

  }

  @Override
  public int getSize() {

    return 0;
  }

  @Override
  public int getInMemorySize() {

    return 0;
  }

  @Override
  public int getOffHeapSize() {

    return 0;
  }

  @Override
  public int getOnDiskSize() {

    return 0;
  }

  @Override
  public int getTerracottaClusteredSize() {

    return 0;
  }

  @Override
  public long getInMemorySizeInBytes() {

    return 0;
  }

  @Override
  public long getOffHeapSizeInBytes() {

    return 0;
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

    return null;
  }

  @Override
  public boolean containsKey(Object key) {

    return false;
  }

  @Override
  public boolean containsKeyOnDisk(Object key) {

    return false;
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {

    return false;
  }

  @Override
  public boolean containsKeyInMemory(Object key) {

    return false;
  }

  @Override
  public void expireElements() {
    //

  }

  @Override
  public void flush() throws IOException {
    //

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

    return false;
  }

  @Override
  public boolean isClusterCoherent() throws TerracottaNotRunningException {

    return false;
  }

  @Override
  public boolean isNodeCoherent() throws TerracottaNotRunningException {
    //
    return false;
  }

  @Override
  public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
    //

  }

  @Override
  public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException,
      InterruptedException {
    //

  }

  @Override
  public Object getMBean() {

    return null;
  }

  @Override
  public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {

    //
  }

  @Override
  public Results executeQuery(StoreQuery query) throws SearchException {

    return null;
  }

  @Override
  public <T> Attribute<T> getSearchAttribute(String attributeName) {

    return null;
  }

  @Override
  public Map<Object, Element> getAllQuiet(Collection<?> keys) {

    return null;
  }

  @Override
  public Map<Object, Element> getAll(Collection<?> keys) {

    return null;
  }

  @Override
  public void recalculateSize(Object key) {

    //
  }

}
