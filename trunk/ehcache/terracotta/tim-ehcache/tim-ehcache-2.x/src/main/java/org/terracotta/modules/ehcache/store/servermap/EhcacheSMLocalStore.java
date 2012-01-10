/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.servermap;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.terracotta.InternalEhcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;
import com.terracotta.toolkit.collections.servermap.api.adapters.ServerMapLocalStoreAdapter;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EhcacheSMLocalStore extends ServerMapLocalStoreAdapter<Object, Object> {

  private static final Logger   LOGGER  = LoggerFactory.getLogger(EhcacheSMLocalStore.class);
  private final InternalEhcache localStoreCache;
  private final Lock            writeLock;
  private final Lock            readLock;
  private boolean               running = true;

  public EhcacheSMLocalStore(InternalEhcache localStoreCache) {

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    writeLock = lock.writeLock();
    readLock = lock.readLock();

    this.localStoreCache = localStoreCache;
    this.localStoreCache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter() {
      @Override
      public void dispose() {
        cacheDisposed();
      }
    });
  }

  @Override
  public boolean addListener(ServerMapLocalStoreListener<Object, Object> listener) {
    return localStoreCache.getCacheEventNotificationService()
        .registerListener(new ServerMapLocalStoreEhcacheListenerAdapter(listener));
  }

  @Override
  public boolean removeListener(ServerMapLocalStoreListener<Object, Object> listener) {
    return localStoreCache.getCacheEventNotificationService()
        .unregisterListener(new ServerMapLocalStoreEhcacheListenerAdapter(listener));
  }

  @Override
  public Object get(Object key) {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring get for key: " + key + " as inner cache is not alive.");
        return null;
      }

      Element element = localStoreCache.get(key);
      return element == null ? null : element.getObjectValue();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public List<Object> getKeys() {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring getKeySet as inner cache is not alive.");
        return Collections.EMPTY_LIST;
      }
      return localStoreCache.getKeys();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object put(Object key, Object value) throws ServerMapLocalStoreFullException {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring put for key: " + key + ", value: " + value + " as inner cache is not alive.");
        return null;
      }
      Element element = localStoreCache.get(key);
      localStoreCache.put(new Element(key, value));
      return element == null ? null : element.getObjectValue();
    } catch (CacheException e) {
      handleCacheException(e);
      throw e;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object replace(Object key, Object oldValue, Object newValue) {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring put for key: " + key + ", value: " + oldValue + " as inner cache is not alive.");
        return null;
      }

      Element element = localStoreCache.get(key);
      if (element == null || !oldValue.equals(element.getObjectValue())) { return null; }

      if (!localStoreCache.remove(key)) { return null; }

      Element newElement = new Element(key, newValue);
      newElement.setEternal(element.isEternal());
      localStoreCache.put(newElement);
      return element.getObjectValue();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object remove(Object key) {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring remove for key " + key + " as inner cache is not alive.");
        return null;
      }
      Element element = localStoreCache.removeAndReturnElement(key);
      return element == null ? null : element.getObjectValue();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object remove(Object key, Object value) {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring remove for key " + key + " as inner cache is not alive.");
        return null;
      }
      Element element = localStoreCache.get(key);
      if (element == null || !value.equals(element.getObjectValue())) { return null; }
      boolean removed = localStoreCache.remove(key);
      if (removed) { return element.getObjectValue(); }
      return null;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getMaxEntriesLocalHeap() {
    return (int) localStoreCache.getCacheConfiguration().getMaxEntriesLocalHeap();
  }

  @Override
  public void setMaxEntriesLocalHeap(int newValue) {
    localStoreCache.getCacheConfiguration().setMaxEntriesLocalHeap(newValue);
  }

  private void handleCacheException(CacheException ce) throws ServerMapLocalStoreFullException, CacheException {
    Throwable rootCause = getRootCause(ce);
    if (rootCause.getClass().getName().contains("OversizeMappingException")
        || rootCause.getClass().getName().contains("CrossPoolEvictionException")) {
      throw new ServerMapLocalStoreFullException();
    } else {
      throw ce;
    }
  }

  private Throwable getRootCause(Throwable throwable) {
    Throwable t = throwable;
    if (t == null) { throw new AssertionError("Tried to find the root cause of null"); }
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  @Override
  public void unpinAll() {
    readLock.lock();
    try {
      localStoreCache.unpinAll();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean isPinned(Object key) {
    readLock.lock();
    try {
      return localStoreCache.isPinned(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setPinned(Object key, boolean pinned) {
    readLock.lock();
    try {
      localStoreCache.setPinned(key, pinned);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void clear() {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring clear as inner cache is not alive.");
        return;
      }
      localStoreCache.removeAll();
    } finally {
      readLock.unlock();
    }
  }

  boolean isCacheAlive() {
    return running && localStoreCache.getStatus() == Status.STATUS_ALIVE;
  }

  @Override
  public long getOnHeapSizeInBytes() {
    readLock.lock();
    try {
      return localStoreCache.calculateInMemorySize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public long getOffHeapSizeInBytes() {
    readLock.lock();
    try {
      return localStoreCache.calculateOffHeapSize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getOffHeapSize() {
    readLock.lock();
    try {
      return (int) localStoreCache.getOffHeapStoreSize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getOnHeapSize() {
    readLock.lock();
    try {
      return (int) localStoreCache.getMemoryStoreSize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getSize() {
    readLock.lock();
    try {
      return localStoreCache.getSize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void dispose() {
    cacheDisposed();
    localStoreCache.dispose();
  }

  @Override
  public boolean containsKeyOnHeap(Object key) {
    readLock.lock();
    try {
      return localStoreCache.isElementInMemory(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    readLock.lock();
    try {
      // Offheap has everything in the local cache, so we just need to verify that the key is anywhere in the cache
      return localStoreCache.isKeyInCache(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap) {
    localStoreCache.getCacheConfiguration().setMaxBytesLocalHeap(newMaxBytesLocalHeap);
  }

  @Override
  public long getMaxBytesLocalHeap() {
    return localStoreCache.getCacheConfiguration().getMaxBytesLocalHeap();
  }

  /**
   * Used in tests
   */
  Ehcache getLocalEhcache() {
    return localStoreCache;
  }

  @Override
  public void recalculateSize(Object key) {
    readLock.lock();
    try {
      if (!isCacheAlive()) {
        LOGGER.info("Ignoring recalculateSize as inner cache is not alive.");
        return;
      }
      localStoreCache.recalculateSize(key);
    } finally {
      readLock.unlock();
    }
  }

  private void cacheDisposed() {
    writeLock.lock();
    running = false;
    writeLock.unlock();
  }

}
