/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.servermap;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.terracotta.InternalEhcache;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;
import com.terracotta.toolkit.collections.servermap.api.adapters.ServerMapLocalStoreAdapter;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EhcacheSMLocalStore extends ServerMapLocalStoreAdapter<Object, Object> {

  private final InternalEhcache            localStoreCache;
  private final Lock                       writeLock;
  private final Lock                       readLock;
  private boolean                          running = true;
  private final OfflineEhcacheSMLocalStore offlineStore;
  private final OnlineEhcacheSMLocalStore  onlineStore;

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
    offlineStore = new OfflineEhcacheSMLocalStore(localStoreCache);
    onlineStore = new OnlineEhcacheSMLocalStore(localStoreCache);
  }

  private void cacheDisposed() {
    writeLock.lock();
    running = false;
    writeLock.unlock();
  }

  private ServerMapLocalStore getActiveStore() {
    readLock.lock();
    try {
      if (running && localStoreCache.getStatus() == Status.STATUS_ALIVE) {
        return onlineStore;
      } else {
        return offlineStore;
      }
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean addListener(ServerMapLocalStoreListener<Object, Object> listener) {
    return getActiveStore().addListener(listener);
  }

  @Override
  public boolean removeListener(ServerMapLocalStoreListener<Object, Object> listener) {
    return getActiveStore().removeListener(listener);
  }

  @Override
  public Object get(Object key) {
    return getActiveStore().get(key);
  }

  @Override
  public List<Object> getKeys() {
    return getActiveStore().getKeys();
  }

  @Override
  public Object put(Object key, Object value) throws ServerMapLocalStoreFullException {
    return getActiveStore().put(key, value);
  }

  @Override
  public Object replace(Object key, Object oldValue, Object newValue) {
    return getActiveStore().replace(key, oldValue, newValue);
  }

  @Override
  public Object remove(Object key) {
    return getActiveStore().remove(key);
  }

  @Override
  public Object remove(Object key, Object value) {
    return getActiveStore().remove(key, value);
  }

  @Override
  public int getMaxEntriesLocalHeap() {
    return getActiveStore().getMaxEntriesLocalHeap();
  }

  @Override
  public void setMaxEntriesLocalHeap(int newValue) {
    getActiveStore().setMaxEntriesLocalHeap(newValue);
  }

  @Override
  public void unpinAll() {
    getActiveStore().unpinAll();
  }

  @Override
  public boolean isPinned(Object key) {
    return getActiveStore().isPinned(key);
  }

  @Override
  public void setPinned(Object key, boolean pinned) {
    getActiveStore().setPinned(key, pinned);
  }

  @Override
  public void clear() {
    getActiveStore().clear();
  }

  @Override
  public long getOnHeapSizeInBytes() {
    return getActiveStore().getOnHeapSizeInBytes();
  }

  @Override
  public long getOffHeapSizeInBytes() {
    return getActiveStore().getOffHeapSizeInBytes();
  }

  @Override
  public int getOffHeapSize() {
    return getActiveStore().getOffHeapSize();
  }

  @Override
  public int getOnHeapSize() {
    return getActiveStore().getOnHeapSize();
  }

  @Override
  public int getSize() {
    return getActiveStore().getSize();
  }

  @Override
  public void dispose() {
    cacheDisposed();
    localStoreCache.dispose();
  }

  @Override
  public boolean containsKeyOnHeap(Object key) {
    return getActiveStore().containsKeyOnHeap(key);
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    return getActiveStore().containsKeyOffHeap(key);
  }

  @Override
  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap) {
    getActiveStore().setMaxBytesLocalHeap(newMaxBytesLocalHeap);
  }

  @Override
  public long getMaxBytesLocalHeap() {
    return getActiveStore().getMaxBytesLocalHeap();
  }

  /**
   * Used in tests
   */
  Ehcache getLocalEhcache() {
    return localStoreCache;
  }

  @Override
  public void recalculateSize(Object key) {
    getActiveStore().recalculateSize(key);
  }

}
