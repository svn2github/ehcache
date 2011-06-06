/*
 * All content copyright (c) 2010 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.store;

import java.io.IOException;
import java.util.List;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 *
 * @author Chris Dennis
 */
public abstract class FrontEndCacheTier<T extends Store, U extends Store> extends AbstractStore {

  protected final T cache;
  protected final U authority;

  public FrontEndCacheTier(T cache, U authority) {
    this.cache = cache;
    this.authority = authority;
  }

  protected Element copyElementForReadIfNeeded(Element element) {
    return element;
  }

  protected Element copyElementForWriteIfNeeded(Element element) {
    return element;
  }

  protected boolean isCacheFull() {
    return true;
  }

  protected boolean isAuthorityHandlingPinnedElements() {
    return false;
  }

  public Element get(Object key) {
    readLock(key);
    try {
      Element e = cache.get(key);
      if (e == null) {
        e = authority.get(key);
        if (e != null) {
          cache.put(e);
        }
      }
      return copyElementForReadIfNeeded(e);
    } finally {
      readUnlock(key);
    }
  }

  public Element getQuiet(Object key) {
    readLock(key);
    try {
      Element e = cache.getQuiet(key);
      if (e == null) {
        e = authority.getQuiet(key);
        if (e != null) {
          cache.put(e);
        }
      }
      return copyElementForReadIfNeeded(e);
    } finally {
      readUnlock(key);
    }
  }

  public boolean put(Element e) {
    Object key = e.getObjectKey();

    writeLock(key);
    try {
      Element copy = copyElementForWriteIfNeeded(e);
      if (!isAuthorityHandlingPinnedElements() && e.isPinned()) {
        boolean put = cache.put(copy);
        authority.remove(key);
        return put;
      }

      if (!isCacheFull() || cache.remove(key) != null) {
        cache.put(copy);
      }
      return authority.put(copy);
    } finally {
      writeUnlock(key);
    }
  }

  public boolean putWithWriter(Element e, CacheWriterManager writer) {
    Object key = e.getObjectKey();

    writeLock(key);
    try {
      Element copy = copyElementForWriteIfNeeded(e);
      if (!isAuthorityHandlingPinnedElements() && e.isPinned()) {
        boolean put = cache.putWithWriter(copy, writer);
        authority.remove(key);
        return put;
      }

      if (!isCacheFull() || cache.remove(key) != null) {
        cache.put(copy);
      }
      return authority.putWithWriter(copy, writer);
    } finally {
      writeUnlock(key);
    }
  }

  public Element remove(Object key) {
    writeLock(key);
    try {
      cache.remove(key);
      return copyElementForReadIfNeeded(authority.remove(key));
    } finally {
      writeUnlock(key);
    }
  }

  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
    writeLock(key);
    try {
      cache.remove(key);
      return copyElementForReadIfNeeded(authority.removeWithWriter(key, writerManager));
    } finally {
      writeUnlock(key);
    }
  }

  public Element putIfAbsent(Element e) throws NullPointerException {
    Object key = e.getObjectKey();

    writeLock(key);
    try {
      Element copy = copyElementForWriteIfNeeded(e);
      if (!isAuthorityHandlingPinnedElements() && e.isPinned()) {
        if (authority.containsKey(key)) {
          return null;
        }
        Element put = cache.putIfAbsent(copy);
        if (put != null) {
          authority.remove(key);
        }
        return copyElementForReadIfNeeded(put);
      }

      Element old = authority.putIfAbsent(copy);
      if (old == null) {
        if (!isCacheFull()) {
          cache.put(copy);
        } else {
          cache.remove(key);
        }
      }
      return copyElementForReadIfNeeded(old);
    } finally {
      writeUnlock(key);
    }
  }

  public Element removeElement(Element e, ElementValueComparator comparator) throws NullPointerException {
    Object key = e.getObjectKey();

    writeLock(key);
    try {
      cache.remove(e.getObjectKey());
      return copyElementForReadIfNeeded(authority.removeElement(e, comparator));
    } finally {
      writeUnlock(key);
    }
  }

  public boolean replace(Element old, Element e, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
    Object key = old.getObjectKey();

    writeLock(key);
    try {
      Element copy = copyElementForWriteIfNeeded(e);
      if (!isAuthorityHandlingPinnedElements() && e.isPinned()) {
        if (!authority.containsKey(key)) {
          return false;
        } else {
          cache.put(authority.get(key));
        }

        boolean replaced = cache.replace(old, copy, comparator);
        if (replaced) {
          authority.remove(key);
        }
        return replaced;
      }

      cache.remove(old.getObjectKey());
      return authority.replace(old, copy, comparator);
    } finally {
      writeUnlock(key);
    }
  }

  public Element replace(Element e) throws NullPointerException {
    Object key = e.getObjectKey();

    writeLock(key);
    try {
      Element copy = copyElementForWriteIfNeeded(e);
      if (!isAuthorityHandlingPinnedElements() && e.isPinned()) {
        if (!authority.containsKey(key)) {
          return null;
        } else {
          cache.put(authority.get(key));
        }

        Element replaced = cache.replace(copy);
        if (replaced != null) {
          authority.remove(key);
        }
        return copyElementForReadIfNeeded(replaced);
      }

      cache.remove(e.getObjectKey());
      return copyElementForReadIfNeeded(authority.replace(copy));
    } finally {
      writeUnlock(key);
    }
  }

  public boolean containsKey(Object key) {
    readLock(key);
    try {
      return cache.containsKey(key) || authority.containsKey(key);
    } finally {
      readUnlock(key);
    }
  }

  public boolean containsKeyOnDisk(Object key) {
    readLock(key);
    try {
      return cache.containsKeyOnDisk(key) || authority.containsKeyOnDisk(key);
    } finally {
      readUnlock(key);
    }
  }

  public boolean containsKeyOffHeap(Object key) {
    readLock(key);
    try {
      return cache.containsKeyOffHeap(key) || authority.containsKeyOffHeap(key);
    } finally {
      readUnlock(key);
    }
  }

  public boolean containsKeyInMemory(Object key) {
    readLock(key);
    try {
      return cache.containsKeyInMemory(key) || authority.containsKeyInMemory(key);
    } finally {
      readUnlock(key);
    }
  }

  public List<?> getKeys() {
    readLock();
    try {
      return authority.getKeys();
    } finally {
      readUnlock();
    }
  }

  public void removeAll() throws CacheException {
    writeLock();
    try {
      cache.removeAll();
      authority.removeAll();
    } finally {
      writeUnlock();
    }
  }

  public void dispose() {
    cache.dispose();
    authority.dispose();
  }

  public int getSize() {
    readLock();
    try {
      return Math.max(cache.getSize(), authority.getSize() + cache.getPinnedCount());
    } finally {
      readUnlock();
    }
  }

  public int getInMemorySize() {
    readLock();
    try {
      return authority.getInMemorySize() + cache.getInMemorySize();
    } finally {
      readUnlock();
    }
  }

  public int getOffHeapSize() {
    readLock();
    try {
      return authority.getOffHeapSize() + cache.getOffHeapSize();
    } finally {
      readUnlock();
    }
  }

  public int getOnDiskSize() {
    readLock();
    try {
      return authority.getOnDiskSize() + cache.getOnDiskSize();
    } finally {
      readUnlock();
    }
  }

  public int getTerracottaClusteredSize() {
    readLock();
    try {
      return authority.getTerracottaClusteredSize() + cache.getTerracottaClusteredSize();
    } finally {
      readUnlock();
    }
  }

  public long getInMemorySizeInBytes() {
    readLock();
    try {
      return authority.getInMemorySizeInBytes() + cache.getInMemorySizeInBytes();
    } finally {
      readUnlock();
    }
  }

  public long getOffHeapSizeInBytes() {
    readLock();
    try {
      return authority.getOffHeapSizeInBytes() + cache.getOffHeapSizeInBytes();
    } finally {
      readUnlock();
    }
  }

  public long getOnDiskSizeInBytes() {
    readLock();
    try {
      return authority.getOnDiskSizeInBytes() + cache.getOnDiskSizeInBytes();
    } finally {
      readUnlock();
    }

  }

  public void expireElements() {
    writeLock();
    try {
      authority.expireElements();
      cache.expireElements();
    } finally {
      writeUnlock();
    }
  }

  public int getPinnedCount() {
    return cache.getPinnedCount();
  }

    //TODO : is this correct?
  public void flush() throws IOException {
    cache.flush();
    authority.flush();
  }

  public boolean bufferFull() {
    return cache.bufferFull() || authority.bufferFull();
  }

  public abstract void readLock(Object key);

  public abstract void readUnlock(Object key);

  public abstract void writeLock(Object key);

  public abstract void writeUnlock(Object key);

  public abstract void readLock();

  public abstract void readUnlock();

  public abstract void writeLock();

  public abstract void writeUnlock();
}
