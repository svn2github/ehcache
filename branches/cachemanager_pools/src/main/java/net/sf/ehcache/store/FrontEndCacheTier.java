/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.store;

import java.io.IOException;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Abstract class for stores which combine two other stores, one caching the other (aka authority)'s elements.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 */
public abstract class FrontEndCacheTier<T extends Store, U extends Store> extends AbstractStore {

    protected final T cache;
    protected final U authority;

    public FrontEndCacheTier(T cache, U authority) {
        this.cache = cache;
        this.authority = authority;
    }

    /**
     * Perform copy on read.
     * @param element the element to copy
     * @return the copied element
     */
    protected abstract Element copyElementForReadIfNeeded(Element element);

    /**
     * Perform copy on write
     * @param element the element to copy
     * @return the copied element
     */
    protected abstract Element copyElementForWriteIfNeeded(Element element);

    /**
     * Check if the caching store is full
     * @return true if the caching store is full, otherwise false.
     */
    protected abstract boolean isCacheFull();

    /**
     * Check if the authority can handle pinned elements. The default implementation returns false.
     * @return true if the authority can handle pinned elements, false otherwise.
     */
    protected boolean isAuthorityHandlingPinnedElements() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        writeLock(key);
        try {
            cache.remove(key);
            return copyElementForReadIfNeeded(authority.remove(key));
        } finally {
            writeUnlock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        writeLock(key);
        try {
            cache.remove(key);
            return copyElementForReadIfNeeded(authority.removeWithWriter(key, writerManager));
        } finally {
            writeUnlock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        readLock(key);
        try {
            return cache.containsKey(key) || authority.containsKey(key);
        } finally {
            readUnlock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        readLock(key);
        try {
            return cache.containsKeyOnDisk(key) || authority.containsKeyOnDisk(key);
        } finally {
            readUnlock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        readLock(key);
        try {
            return cache.containsKeyOffHeap(key) || authority.containsKeyOffHeap(key);
        } finally {
            readUnlock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        readLock(key);
        try {
            return cache.containsKeyInMemory(key) || authority.containsKeyInMemory(key);
        } finally {
            readUnlock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<?> getKeys() {
        readLock();
        try {
            return authority.getKeys();
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        writeLock();
        try {
            cache.removeAll();
            authority.removeAll();
        } finally {
            writeUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        cache.dispose();
        authority.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        readLock();
        try {
            return Math.max(cache.getSize(), authority.getSize() + cache.getPinnedCount());
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        readLock();
        try {
            return authority.getInMemorySize() + cache.getInMemorySize();
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        readLock();
        try {
            return authority.getOffHeapSize() + cache.getOffHeapSize();
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        readLock();
        try {
            return authority.getOnDiskSize() + cache.getOnDiskSize();
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        readLock();
        try {
            return authority.getTerracottaClusteredSize() + cache.getTerracottaClusteredSize();
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        readLock();
        try {
            return authority.getInMemorySizeInBytes() + cache.getInMemorySizeInBytes();
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        readLock();
        try {
            return authority.getOffHeapSizeInBytes() + cache.getOffHeapSizeInBytes();
        } finally {
            readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        readLock();
        try {
            return authority.getOnDiskSizeInBytes() + cache.getOnDiskSizeInBytes();
        } finally {
            readUnlock();
        }

    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        writeLock();
        try {
            authority.expireElements();
            cache.expireElements();
        } finally {
            writeUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getPinnedCount() {
        return cache.getPinnedCount();
    }

    /**
     * {@inheritDoc}
     */
    //TODO : is this correct?
    public void flush() throws IOException {
        cache.flush();
        authority.flush();
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return cache.bufferFull() || authority.bufferFull();
    }

    /**
     * Acquire the read lock of the specified key
     * @param key the key to read-lock
     */
    public abstract void readLock(Object key);

    /**
     * Unlock the read lock of the specified key
     * @param key the key to read-unlock
     */
    public abstract void readUnlock(Object key);

    /**
     * Acquire the write lock of the specified key
     * @param key the key to write-lock
     */
    public abstract void writeLock(Object key);

    /**
     * Unlock the write lock of the specified key
     * @param key the key to write-unlock
     */
    public abstract void writeUnlock(Object key);

    /**
     * Acquire the read lock of all keys
     */
    public abstract void readLock();

    /**
     * Unlock the read lock of all keys
     */
    public abstract void readUnlock();

    /**
     * Acquire the write lock of all keys
     */
    public abstract void writeLock();

    /**
     * Unlock the write lock of all keys
     */
    public abstract void writeUnlock();

}
