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
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.ReadWriteLockSync;
import net.sf.ehcache.concurrent.StripedReadWriteLock;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
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
public abstract class FrontEndCacheTier<T extends TierableStore, U extends TierableStore> extends AbstractStore {

    private static final int DEFAULT_LOCK_STRIPE_COUNT = 128;

    /**
     * The cache tier store
     */
    protected final T cache;

    /**
     * The authority tier store
     */
    protected final U authority;

    private final StripedReadWriteLock masterLocks;
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
        if (authority instanceof StripedReadWriteLockProvider) {
            masterLocks = ((StripedReadWriteLockProvider) authority).createStripedReadWriteLock();
        } else {
            masterLocks = new StripedReadWriteLockSync(DEFAULT_LOCK_STRIPE_COUNT);
        }
    }

    /**
     * Perform copy on read on an element if configured
     *
     * @param element the element to copy for read
     * @return a copy of the element with the reconstructed original value
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
     * Perform copy on write on an element if configured
     *
     * @param element the element to copy for write
     * @return a copy of the element with a storage-ready value
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
     * {@inheritDoc}
     */
    public Element get(Object key) {
        if (key == null) {
            return null;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
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
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        if (key == null) {
            return null;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
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
            lock.unlock();
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

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            Element copy = copyElementForWriteIfNeeded(e);
            final boolean put = authority.put(copy);
            try {
                cache.fill(copy);
            } catch (OutOfMemoryError oome) {
                authority.remove(e.getKey());
                throw oome;
            }
            return put;
        } finally {
            lock.unlock();
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

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            Element copy = copyElementForWriteIfNeeded(e);
            final boolean put = authority.putWithWriter(copy, writer);
            try {
                cache.fill(copy);
            } catch (OutOfMemoryError oome) {
                authority.remove(e.getKey());
                throw oome;
            }
            return put;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        if (key == null) {
            return null;
        }

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            cache.remove(key);
            return copyElementForReadIfNeeded(authority.remove(key));
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        if (key == null) {
            return null;
        }

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            cache.remove(key);
            return copyElementForReadIfNeeded(authority.removeWithWriter(key, writerManager));
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element e) throws NullPointerException {
        Object key = e.getObjectKey();

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            Element copy = copyElementForWriteIfNeeded(e);
            Element old = authority.putIfAbsent(copy);
            if (old == null) {
                try {
                    cache.fill(copy);
                } catch (OutOfMemoryError oome) {
                    authority.remove(copy.getKey());
                    throw oome;
                }
            }
            return copyElementForReadIfNeeded(old);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element e, ElementValueComparator comparator) throws NullPointerException {
        Object key = e.getObjectKey();

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            cache.remove(e.getObjectKey());
            return copyElementForReadIfNeeded(authority.removeElement(e, comparator));
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element e, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        Object key = old.getObjectKey();

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            Element copy = copyElementForWriteIfNeeded(e);
            cache.remove(old.getObjectKey());
            return authority.replace(old, copy, comparator);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element e) throws NullPointerException {
        Object key = e.getObjectKey();

        Lock lock = getLockFor(key).writeLock();
        lock.lock();
        try {
            Element copy = copyElementForWriteIfNeeded(e);
            cache.remove(e.getObjectKey());
            return copyElementForReadIfNeeded(authority.replace(copy));
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
        try {
            return cache.containsKey(key) || authority.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        if (key == null) {
            return false;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
        try {
            return cache.containsKeyOnDisk(key) || authority.containsKeyOnDisk(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        if (key == null) {
            return false;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
        try {
            return cache.containsKeyOffHeap(key) || authority.containsKeyOffHeap(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        if (key == null) {
            return false;
        }

        Lock lock = getLockFor(key).readLock();
        lock.lock();
        try {
            return cache.containsKeyInMemory(key) || authority.containsKeyInMemory(key);
        } finally {
            lock.unlock();
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
            return Math.max(cache.getSize(), authority.getSize());
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
        return authority.getInMemorySizeInBytes() + cache.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return authority.getOffHeapSizeInBytes() + cache.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return authority.getOnDiskSizeInBytes() + cache.getOnDiskSizeInBytes();
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

    private void readLock() {
        for (ReadWriteLockSync lock : getAllLocks()) {
            lock.lock(LockType.READ);
        }
    }

    private void readUnlock() {
        for (ReadWriteLockSync lock : getAllLocks()) {
            lock.unlock(LockType.READ);
        }
    }

    private void writeLock() {
        for (ReadWriteLockSync lock : getAllLocks()) {
            lock.lock(LockType.WRITE);
        }
    }

    private void writeUnlock() {
        for (ReadWriteLockSync lock : getAllLocks()) {
            lock.unlock(LockType.WRITE);
        }
    }

    /**
     * Returns the ReadWriteLock guarding this key.
     * 
     * @param key key of interest
     * @return lock for the supplied key
     */
    protected ReadWriteLock getLockFor(Object key) {
        return masterLocks.getLockForKey(key);
    }

    private List<ReadWriteLockSync> getAllLocks() {
        return masterLocks.getAllSyncs();
    }

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
    public final Object getInternalContext() {
        return masterLocks;
    }

    /**
     * Checks whether the element can be safely evicted.
     * It is done by checking whether we can lock the master lock for that Element's key and, if we could, remove that key from all other tiers,
     * but the lowest.
     * <p>Failing to obey this, might result in firing an Element Evicted Event, while it is still present in higher tiers</p>
     * @param e The element we want to evict
     * @return true, if it can be evicted, false otherwise
     */
    public boolean isEvictionCandidate(final Element e) {
        Object key = e.getObjectKey();
        Lock lockForKey = masterLocks.getLockForKey(key).writeLock();
        if (lockForKey.tryLock()) {
            try {
                cache.removeIfTierNotPinned(key);
                return true;
            } finally {
                lockForKey.unlock();
            }
        } else {
            return false;
        }
    }

    /**
     * Checks whether the key is held in the fronting cache
     * @param key the key to check for
     * @return true if cached, false otherwise
     */
    public boolean isCached(final Object key) {
        return cache.containsKey(key);
    }

    /**
     * Whether evicting this from the cache should fire when evicting from upper tiers
     * @param key the key to the element
     * @return true if we should fire, otherwise false
     */
    public boolean notifyEvictionFromCache(final Serializable key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAbortedSizeOf() {
        return cache.hasAbortedSizeOf() || authority.hasAbortedSizeOf();
    }
}
