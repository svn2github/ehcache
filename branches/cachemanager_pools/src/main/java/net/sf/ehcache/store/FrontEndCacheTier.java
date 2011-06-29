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
import net.sf.ehcache.Status;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Abstract class for stores which combine two other stores, one caching the other (aka authority)'s elements.
 *
 * @param <T> the cache tier store type
 * @param <U> the authority tier store type
 * @author Chris Dennis
 * @author Ludovic Orban
 */
public abstract class FrontEndCacheTier<T extends Store, U extends Store> extends AbstractStore {

    /**
     * The cache tier store
     */
    protected final T cache;

    /**
     * The authority tier store
     */
    protected final U authority;

    private final boolean copyOnRead;
    private final boolean copyOnWrite;
    private final ReadWriteCopyStrategy<Element> copyStrategy;

    /**
     * Constructor for FrontEndCacheTier
     * 
     * @param cache the caching tier
     * @param authority the authority tier
     * @param copyStrategy the copyStrategy to use
     * @param copyOnWrite whether to copy on writes, false otherwise
     * @param copyOnRead whether to copy on reads, false otherwise
     */
    public FrontEndCacheTier(T cache, U authority, ReadWriteCopyStrategy<Element> copyStrategy, boolean copyOnWrite, boolean copyOnRead) {
        this.cache = cache;
        this.authority = authority;
        this.copyStrategy = copyStrategy;
        this.copyOnWrite = copyOnWrite;
        this.copyOnRead = copyOnRead;
    }

    /**
     * {@inheritDoc}
     */
    protected Element copyElementForReadIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForRead(element);
        } else if (copyOnRead) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Element copyElementForWriteIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element);
        } else if (copyOnWrite) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * Check if the caching store has not enough room to add an element without provoking an eviction.
     *
     * @param element the element to check against.
     * @return true if the caching store is full, otherwise false.
     */
    protected abstract boolean cacheHasRoomFor(Element element);

    /**
     * Check if the authority can handle pinned elements. The default implementation returns false.
     * 
     * @return true if the authority can handle pinned elements, false otherwise.
     */
    protected boolean isAuthorityHandlingPinnedElements() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        if (key == null) {
            return null;
        }

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
        if (key == null) {
            return null;
        }

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
        if (e == null) {
            return true;
        }

        Object key = e.getObjectKey();

        writeLock(key);
        try {
            Element copy = copyElementForWriteIfNeeded(e);
            if (!isAuthorityHandlingPinnedElements() && e.isPinned()) {
                boolean put = cache.put(copy);
                authority.remove(key);
                return put;
            }

            if (cache.remove(key) != null || cacheHasRoomFor(copy)) {
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
        if (e == null) {
            return true;
        }

        Object key = e.getObjectKey();

        writeLock(key);
        try {
            Element copy = copyElementForWriteIfNeeded(e);
            if (!isAuthorityHandlingPinnedElements() && e.isPinned()) {
                boolean put = cache.putWithWriter(copy, writer);
                authority.remove(key);
                return put;
            }

            if (cache.remove(key) != null || cacheHasRoomFor(copy)) {
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
        if (key == null) {
            return null;
        }

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
        if (key == null) {
            return null;
        }

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
            } else {
                Element old = authority.putIfAbsent(copy);
                if (old == null) {
                    if (cache.remove(key) != null || cacheHasRoomFor(copy)) {
                        cache.put(copy);
                    }
                }
                return copyElementForReadIfNeeded(old);
            }
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
                boolean justCached = false;
                if (authority.containsKey(key)) {
                    Element element = authority.get(key);
                    element.setPinned(true);
                    cache.put(element);
                    justCached = true;
                }

                boolean replaced = cache.replace(old, copy, comparator);
                if (replaced) {
                    authority.remove(key);
                } else if (justCached) {
                    cache.remove(key);
                }
                return replaced;
            } else {
                cache.remove(old.getObjectKey());
                return authority.replace(old, copy, comparator);
            }

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
                boolean justCached = false;
                if (authority.containsKey(key)) {
                    Element element = authority.get(key);
                    element.setPinned(true);
                    cache.put(element);
                    justCached = true;
                }

                Element replaced = cache.replace(copy);
                if (replaced != null) {
                    authority.remove(key);
                } else if (justCached) {
                    cache.remove(key);
                }
                return copyElementForReadIfNeeded(replaced);
            } else {
                cache.remove(e.getObjectKey());
                return copyElementForReadIfNeeded(authority.replace(copy));
            }
        } finally {
            writeUnlock(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }

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
        if (key == null) {
            return false;
        }

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
        if (key == null) {
            return false;
        }

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
        if (key == null) {
            return false;
        }

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

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        //TODO this might be wrong...
        return authority.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return cache.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        cache.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return authority.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return authority.getMBean();
    }
}
