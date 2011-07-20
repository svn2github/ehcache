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

/**
 *
 */
package net.sf.ehcache.store.disk;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.Role;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.util.ratestatistics.AtomicRateStatistic;
import net.sf.ehcache.util.ratestatistics.RateStatistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Segment implementation used in LocalStore.
 * <p>
 * The segment extends ReentrantReadWriteLock to allow read locking on read operations.
 * In addition to the typical CHM-like methods, this classes additionally supports
 * replacement under a read lock - which is accomplished using an atomic CAS on the
 * associated HashEntry.
 * 
 * @author Chris Dennis
 * @author Ludovic Orban
 */
public class Segment extends ReentrantReadWriteLock {

    private static final Logger LOG = LoggerFactory.getLogger(Segment.class.getName());

    private static final float LOAD_FACTOR = 0.75f;
    private static final int MAXIMUM_CAPACITY = Integer.highestOneBit(Integer.MAX_VALUE);
    
    /**
     * Count of elements in the map.
     * <p>
     * A volatile reference is needed here for the same reasons as in the table reference.
     */
    protected volatile int count;

    /**
     * Mod-count used to track concurrent modifications when doing size calculations or iterating over the store.
     * <p>
     * Note that we don't actually have any iterators yet...
     */
    protected int modCount;

    /**
     * The primary substitute factory.
     * <p>
     * This is the substitute type used to store <code>Element</code>s when they are first added to the store.
     */
    private final DiskStorageFactory disk;

    /**
     * Table of HashEntry linked lists, indexed by the least-significant bits of the spread-hash value.
     * <p>
     * A volatile reference is needed to ensure the visibility of table changes made during rehash operations to size operations.
     * Key operations are done under read-locks so there is no need for volatility in that regard.  Hence if we switched to read-locked
     * size operations, we wouldn't need a volatile reference here.
     */
    private volatile HashEntry[] table;

    /**
     * Size at which the next rehashing of this Segment should occur
     */
    private int threshold;

    private final RateStatistic diskHitRate = new AtomicRateStatistic(1000, TimeUnit.MILLISECONDS);
    private final RateStatistic diskMissRate = new AtomicRateStatistic(1000, TimeUnit.MILLISECONDS);
    private final PoolAccessor onHeapPoolAccessor;
    private final PoolAccessor onDiskPoolAccessor;
    private volatile boolean cachePinned;

    /**
     * Create a Segment with the given initial capacity, load-factor, primary element substitute factory, and identity element substitute factory.
     * <p>
     * An identity element substitute factory is specified at construction time because only one subclass of IdentityElementProxyFactory
     * can be used with a Segment.  Without this requirement the mapping between bare {@link net.sf.ehcache.Element} instances and the factory
     * responsible for them would be ambiguous.
     * <p>
     * If a <code>null</code> identity element substitute factory is specified then encountering a raw element (i.e. as a result of using an
     * identity element substitute factory) will result in a null pointer exception during decode.
     *
     * @param initialCapacity initial capacity of store
     * @param loadFactor fraction of capacity at which rehash occurs
     * @param primary primary element substitute factory
     * @param cacheConfiguration the cache configuration
     * @param onHeapPoolAccessor the pool tracking on-heap usage
     * @param onDiskPoolAccessor the pool tracking on-disk usage
     */
    public Segment(int initialCapacity, float loadFactor, DiskStorageFactory primary,
                   CacheConfiguration cacheConfiguration,
                   PoolAccessor onHeapPoolAccessor, PoolAccessor onDiskPoolAccessor) {
        this.onHeapPoolAccessor = onHeapPoolAccessor;
        this.onDiskPoolAccessor = onDiskPoolAccessor;
        this.table = new HashEntry[initialCapacity];
        this.threshold = (int) (table.length * loadFactor);
        this.modCount = 0;
        this.disk = primary;
        this.cachePinned = determineCachePinned(cacheConfiguration);
    }

    private boolean determineCachePinned(CacheConfiguration cacheConfiguration) {
        PinningConfiguration pinningConfiguration = cacheConfiguration.getPinningConfiguration();
        if (pinningConfiguration == null) {
            return false;
        }

        switch (pinningConfiguration.getStore()) {
            case LOCALHEAP:
                return false;

            case LOCALMEMORY:
                return false;

            case INCACHE:
                return cacheConfiguration.isOverflowToDisk() || cacheConfiguration.isDiskPersistent();

            default:
                throw new IllegalArgumentException();
        }
    }

    private HashEntry getFirst(int hash) {
        HashEntry[] tab = table;
        return tab[hash & (tab.length - 1)];
    }

    /**
     * Decode the possible DiskSubstitute
     *
     * @param object the DiskSubstitute to decode
     * @return the decoded DiskSubstitute
     */
    private Element decode(Object object) {
        DiskStorageFactory.DiskSubstitute substitute = (DiskStorageFactory.DiskSubstitute) object;
        return substitute.getFactory().retrieve(substitute);
    }

    /**
     * Decode the possible DiskSubstitute, updating the statistics
     *
     * @param object the DiskSubstitute to decode
     * @return the decoded DiskSubstitute
     */
    private Element decodeHit(Object object) {
        DiskStorageFactory.DiskSubstitute substitute = (DiskStorageFactory.DiskSubstitute) object;
        return substitute.getFactory().retrieve(substitute, this);
    }

    /**
     * Free the DiskSubstitute
     *
     * @param object the DiskSubstitute to free
     */
    private void free(Object object) {
        free(object, false);
    }

    /**
     * Free the DiskSubstitute indicating if it could not be faulted
     *
     * @param object the DiskSubstitute to free
     * @param faultFailure true if the DiskSubstitute should be freed because of a fault failure
     */
    private void free(Object object, boolean faultFailure) {
        DiskStorageFactory.DiskSubstitute diskSubstitute = (DiskStorageFactory.DiskSubstitute) object;
        diskSubstitute.getFactory().free(writeLock(), diskSubstitute, faultFailure);
    }

    /**
     * Get the element mapped to this key (or null if there is no mapping for this key)
     *
     * @param key key to lookup
     * @param hash spread-hash for this key
     * @return mapped element
     */
    Element get(Object key, int hash) {
        readLock().lock();
        try {
            // read-volatile
            if (count != 0) {
                HashEntry e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key)) {
                        return decodeHit(e.getElement());
                    }
                    e = e.next;
                }
            }
            miss();
            return null;
        } finally {
            readLock().unlock();
        }
    }

    /**
     * Return the unretrieved (undecoded) value for this key
     *
     * @param key key to lookup
     * @param hash spread-hash for the key
     * @return Element or ElementSubstitute
     */
    Object unretrievedGet(Object key, int hash) {
        readLock().lock();
        try {
            if (count != 0) {
                HashEntry e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key)) {
                        return e.getElement();
                    }
                    e = e.next;
                }
            }
        } finally {
            readLock().unlock();
        }
        return null;
    }

    /**
     * Return true if this segment contains a mapping for this key
     *
     * @param key key to check for
     * @param hash spread-hash for key
     * @return <code>true</code> if there is a mapping for this key
     */
    boolean containsKey(Object key, int hash) {
        readLock().lock();
        try {
            // read-volatile
            if (count != 0) {
                HashEntry e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key)) {
                        return true;
                    }
                    e = e.next;
                }
            }
            return false;
        } finally {
            readLock().unlock();
        }
    }

    /**
     * Replace the element mapped to this key only if currently mapped to the given element.
     *
     *
     * @param key key to map the element to
     * @param hash spread-hash for the key
     * @param oldElement expected element
     * @param newElement element to add
     * @param comparator the comparator to use to compare values
     * @return <code>true</code> on a successful replace
     */
    boolean replace(Object key, int hash, Element oldElement, Element newElement, ElementValueComparator comparator) {
        boolean installed = false;
        DiskStorageFactory.DiskSubstitute encoded = disk.create(newElement);

        writeLock().lock();
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            boolean replaced = false;
            if (e != null && comparator.equals(oldElement, decode(e.getElement()))) {
                replaced = true;
                /*
                 * make sure we re-get from the HashEntry - since the decode in the conditional
                 * may have faulted in a different type - we must make sure we know what type
                 * to do the increment/decrement on.
                 */
                Object onDiskSubstitute = e.getElement();

                long size;
                size = onHeapPoolAccessor.replace(Role.VALUE, onDiskSubstitute, encoded, cachePinned);
                if (size == Long.MAX_VALUE) {
                    LOG.debug("replace3 failed to add on heap");
                    free(encoded);
                    return false;
                } else {
                    LOG.debug("replace3 added {} on heap", size);
                }

                e.setElement(encoded);
                installed = true;
                free(onDiskSubstitute);

                if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                    size = onDiskPoolAccessor.delete(key, null, onDiskSubstitute);
                    LOG.debug("replace3 removed {} from disk", size);
                }
            } else {
                free(encoded);
            }
            return replaced;
        } finally {
            writeLock().unlock();

            if (installed) {
                encoded.installed();
            }
        }
    }

    /**
     * Replace the entry for this key only if currently mapped to some element.
     *
     * @param key key to map the element to
     * @param hash spread-hash for the key
     * @param newElement element to add
     * @return previous element mapped to this key
     */
    Element replace(Object key, int hash, Element newElement) {
        boolean installed = false;
        DiskStorageFactory.DiskSubstitute encoded = disk.create(newElement);

        writeLock().lock();
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            Element oldElement = null;
            if (e != null) {
                Object onDiskSubstitute = e.getElement();

                long size;
                size = onHeapPoolAccessor.replace(Role.VALUE, onDiskSubstitute, encoded, cachePinned);
                if (size == Long.MAX_VALUE) {
                    LOG.debug("replace2 failed to add on heap");
                    free(encoded);
                    return null;
                } else {
                    LOG.debug("replace2 added {} on heap", size);
                }

                e.setElement(encoded);
                installed = true;
                oldElement = decode(onDiskSubstitute);
                free(onDiskSubstitute);

                if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                    size = onDiskPoolAccessor.delete(key, null, onDiskSubstitute);
                    LOG.debug("replace2 removed {} from disk", size);
                }
            } else {
                free(encoded);
            }

            return oldElement;
        } finally {
            writeLock().unlock();

            if (installed) {
                encoded.installed();
            }
        }
    }

    /**
     * Add the supplied mapping.
     * <p>
     * The supplied element is substituted using the primary element proxy factory
     * before being stored in the cache.  If <code>onlyIfAbsent</code> is set
     * then the mapping will only be added if no element is currently mapped
     * to that key.
     *
     * @param key key to map the element to
     * @param hash spread-hash for the key
     * @param element element to store
     * @param onlyIfAbsent if true does not replace existing mappings
     * @return previous element mapped to this key
     */
    Element put(Object key, int hash, Element element, boolean onlyIfAbsent) {
        boolean installed = false;
        DiskStorageFactory.DiskSubstitute encoded = disk.create(element);

        writeLock().lock();
        try {
            long size;
            size = onHeapPoolAccessor.add(key, encoded, HashEntry.newHashEntry(key, hash, null, null), cachePinned);
            if (size < 0) {
                LOG.debug("put failed to add on heap");
                return null;
            } else {
                LOG.debug("put added {} on heap", size);
            }

            // ensure capacity
            if (count + 1 > threshold) {
                rehash();
            }
            HashEntry[] tab = table;
            int index = hash & (tab.length - 1);
            HashEntry first = tab[index];
            HashEntry e = first;
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            Element oldElement;
            if (e != null) {
                Object onDiskSubstitute = e.getElement();
                if (!onlyIfAbsent) {
                    e.setElement(encoded);
                    installed = true;
                    oldElement = decode(onDiskSubstitute);

                    free(onDiskSubstitute);
                    size = onHeapPoolAccessor.delete(key, onDiskSubstitute, HashEntry.newHashEntry(key, hash, null, null));
                    LOG.debug("put updated, deleted {} on heap", size);

                    if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                        size = onDiskPoolAccessor.delete(key, null, onDiskSubstitute);
                        LOG.debug("put updated, deleted {} on disk", size);
                    }
                } else {
                    oldElement = decode(onDiskSubstitute);

                    free(encoded);
                    size = onHeapPoolAccessor.delete(key, encoded, HashEntry.newHashEntry(key, hash, null, null));
                    LOG.debug("put if absent failed, deleted {} on heap", size);
                }
            } else {
                oldElement = null;
                ++modCount;
                tab[index] = HashEntry.newHashEntry(key, hash, first, encoded);
                installed = true;
                // write-volatile
                count = count + 1;
            }
            return oldElement;

        } finally {
            writeLock().unlock();

            if (installed) {
                encoded.installed();
            }
        }
    }


    /**
     * Add the supplied pre-encoded mapping.
     * <p>
     * The supplied encoded element is directly inserted into the segment
     * if there is no other mapping for this key.
     *
     * @param key key to map the element to
     * @param hash spread-hash for the key
     * @param encoded encoded element to store
     * @return <code>true</code> if the encoded element was installed
     * @throws IllegalArgumentException if the supplied key is already present
     */
    boolean putRawIfAbsent(Object key, int hash, Object encoded) throws IllegalArgumentException {
        writeLock().lock();
        try {
            if (!onDiskPoolAccessor.canAddWithoutEvicting(key, null, encoded)) {
                return false;
            }
            if (onHeapPoolAccessor.add(key, encoded, HashEntry.newHashEntry(key, hash, null, null), cachePinned) < 0) {
                return false;
            }
            if (onDiskPoolAccessor.add(key, null, encoded, cachePinned) < 0) {
                onHeapPoolAccessor.delete(key, encoded, HashEntry.newHashEntry(key, hash, null, null));
                return false;
            }

            // ensure capacity
            if (count + 1 > threshold) {
                rehash();
            }
            HashEntry[] tab = table;
            int index = hash & (tab.length - 1);
            HashEntry first = tab[index];
            HashEntry e = first;
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            if (e == null) {
                ++modCount;
                tab[index] = HashEntry.newHashEntry(key, hash, first, encoded);
                // write-volatile
                count = count + 1;
                return true;
            } else {
                onHeapPoolAccessor.delete(key, encoded, HashEntry.newHashEntry(key, hash, null, null));
                onDiskPoolAccessor.delete(key, null, encoded);
                throw new IllegalArgumentException("Duplicate key detected");
            }
        } finally {
            writeLock().unlock();
        }
    }

    private void rehash() {
        HashEntry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity >= MAXIMUM_CAPACITY) {
            return;
        }

        /*
         * Reclassify nodes in each list to new Map.  Because we are
         * using power-of-two expansion, the elements from each bin
         * must either stay at same index, or move with a power of two
         * offset. We eliminate unnecessary node creation by catching
         * cases where old nodes can be reused because their next
         * fields won't change. Statistically, at the default
         * threshold, only about one-sixth of them need cloning when
         * a table doubles. The nodes they replace will be garbage
         * collectable as soon as they are no longer referenced by any
         * reader thread that may be in the midst of traversing table
         * right now.
         */

        HashEntry[] newTable = new HashEntry[oldCapacity << 1];
        threshold = (int)(newTable.length * LOAD_FACTOR);
        int sizeMask = newTable.length - 1;
        for (int i = 0; i < oldCapacity; i++) {
            // We need to guarantee that any existing reads of old Map can
            //  proceed. So we cannot yet null out each bin.
            HashEntry e = oldTable[i];

            if (e != null) {
                HashEntry next = e.next;
                int idx = e.hash & sizeMask;

                //  Single node on list
                if (next == null) {
                    newTable[idx] = e;
                } else {
                    // Reuse trailing consecutive sequence at same slot
                    HashEntry lastRun = e;
                    int lastIdx = idx;
                    for (HashEntry last = next;
                         last != null;
                         last = last.next) {
                        int k = last.hash & sizeMask;
                        if (k != lastIdx) {
                            lastIdx = k;
                            lastRun = last;
                        }
                    }
                    newTable[lastIdx] = lastRun;

                    // Clone all remaining nodes
                    for (HashEntry p = e; p != lastRun; p = p.next) {
                        int k = p.hash & sizeMask;
                        HashEntry n = newTable[k];
                        newTable[k] = HashEntry.newHashEntry(p.key, p.hash, n, p.getElement());
                    }
                }
            }
        }
        table = newTable;
    }

    /**
     * Remove the matching mapping.
     * <p>
     * If <code>value</code> is <code>null</code> then match on the key only,
     * else match on both the key and the value.
     *
     *
     * @param key key to match against
     * @param hash spread-hash for the key
     * @param value optional value to match against
     * @param comparator the comparator to use to compare values
     * @return removed element
     */
    Element remove(Object key, int hash, Element value, ElementValueComparator comparator) {
        writeLock().lock();
        try {
            HashEntry[] tab = table;
            int index = hash & (tab.length - 1);
            HashEntry first = tab[index];
            HashEntry e = first;
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            Element oldValue = null;
            if (e != null) {
                oldValue = decode(e.getElement());
                if (value == null || comparator.equals(value, oldValue)) {
                    // All entries following removed node can stay
                    // in list, but all preceding ones need to be
                    // cloned.
                    ++modCount;
                    HashEntry newFirst = e.next;
                    for (HashEntry p = first; p != e; p = p.next) {
                        newFirst = HashEntry.newHashEntry(p.key, p.hash, newFirst, p.getElement());
                    }
                    tab[index] = newFirst;
                    /*
                     * make sure we re-get from the HashEntry - since the decode in the conditional
                     * may have faulted in a different type - we must make sure we know what type
                     * to do the free on.
                     */
                    Object onDiskSubstitute = e.getElement();
                    free(onDiskSubstitute);

                    long size;
                    size = onHeapPoolAccessor.delete(key, onDiskSubstitute, HashEntry.newHashEntry(key, hash, null, null));
                    LOG.debug("remove deleted {} from heap", size);

                    if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                        size = onDiskPoolAccessor.delete(key, null, onDiskSubstitute);
                        LOG.debug("remove deleted {} from disk", size);
                    }

                    // write-volatile
                    count = count - 1;
                } else {
                    oldValue = null;
                }
            }

            if (oldValue == null) {
                LOG.debug("remove deleted nothing");
            }

            return oldValue;
        } finally {
            writeLock().unlock();
        }
    }

    /**
     * Removes all mappings from this segment.
     */
    void clear() {
        writeLock().lock();
        try {
            if (count != 0) {
                HashEntry[] tab = table;
                for (int i = 0; i < tab.length; i++) {
                    for (HashEntry e = tab[i]; e != null; e = e.next) {
                        free(e.getElement());
                    }
                    tab[i] = null;
                }
                ++modCount;
                // write-volatile
                count = 0;
            }
            onHeapPoolAccessor.clear();
            LOG.debug("cleared heap usage");
            onDiskPoolAccessor.clear();
            LOG.debug("cleared disk usage");
        } finally {
            writeLock().unlock();
        }
    }

    /**
     * Try to atomically switch (CAS) the <code>expect</code> representation of this element for the
     * <code>fault</code> representation.
     * <p>
     * A successful switch will return <code>true</code>, and free the replaced element/element-proxy.
     * A failed switch will return <code>false</code> and free the element/element-proxy which was not
     * installed.  Unlike <code>fault</code> this method can return <code>false</code> if the object
     * could not be installed due to lock contention.
     *
     * @param key key to which this element (proxy) is mapped
     * @param hash the hash of the key
     * @param expect element (proxy) expected
     * @param fault element (proxy) to install
     * @return <code>true</code> if <code>fault</code> was installed
     */
    boolean fault(Object key, int hash, Object expect, Object fault) {
        boolean installed = false;

        writeLock().lock();
        try {
            long size;
            size = onHeapPoolAccessor.replace(Role.VALUE, expect, fault, cachePinned);
            if (size == Long.MAX_VALUE) {
                remove(key, hash, null, null);
                return false;
            } else {
                LOG.debug("fault removed {} from heap", size);
            }
            size = onDiskPoolAccessor.add(key, null, fault, cachePinned);
            if (size < 0) {
                //todo: replace must not fail here but it could if the memory freed by the previous replace has been stolen in the meantime
                // that's why it is forced, even if that could make the pool go over limit
                long deleteSize = onHeapPoolAccessor.replace(Role.VALUE, fault, expect, true);
                LOG.debug("fault failed to add {} on disk, deleted {} from heap", size, deleteSize);
                remove(key, hash, null, null);
                return false;
            } else {
                LOG.debug("fault added {} on disk", size);
            }

            installed = install(key, hash, expect, fault);
            if (installed) {
                return true;
            } else {
                //todo: replace must not fail here but it could if the memory freed by the previous replace has been stolen in the meantime
                // that's why it is forced, even if that could make the pool go over limit
                size = onHeapPoolAccessor.replace(Role.VALUE, fault, expect, true);
                LOG.debug("fault installation failed, deleted {} from heap", size);
                size = onDiskPoolAccessor.delete(key, null, fault);
                LOG.debug("fault installation failed deleted {} from disk", size);

                free(fault, true);
                return false;
            }
        } finally {
            writeLock().unlock();

            if ((installed && fault instanceof DiskStorageFactory.DiskSubstitute)) {
                ((DiskStorageFactory.DiskSubstitute) fault).installed();
            }
        }
    }

    /**
     * Count the number of elements which have been added to the store but haven't been written to disk yet
     *
     * @return the number of elements which have been added to the store but haven't been written to disk yet
     */
    int countOnHeap() {
        readLock().lock();
        try {
            int result = 0;

            if (count != 0) {
                for (HashEntry hashEntry : table) {
                    for (HashEntry e = hashEntry; e != null; e = e.next) {
                        if (e.getElement() instanceof DiskStorageFactory.Placeholder) {
                            result++;
                        }
                    }
                }
            }

            return result;
        } finally {
            readLock().unlock();
        }
    }

    private boolean install(Object key, int hash, Object expect, Object fault) {
        if (count != 0) {
            for (HashEntry e = getFirst(hash); e != null; e = e.next) {
                if (e.hash == hash && key.equals(e.key)) {
                    if (e.casElement(expect, fault)) {
                        free(expect);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Remove the matching mapping.  Unlike the {@link net.sf.ehcache.store.disk.Segment#remove(Object, int, net.sf.ehcache.Element, net.sf.ehcache.store.ElementValueComparator)} method
     * evict does referential comparison of the unretrieved substitute against the argument value.
     * 
     * @param key key to match against
     * @param hash spread-hash for the key
     * @param value optional value to match against
     * @return <code>true</code> on a successful remove
     */
    Element evict(Object key, int hash, Object value) {
        if (writeLock().tryLock()) {
            try {
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry first = tab[index];
                HashEntry e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key))) {
                    e = e.next;
                }

                if (e != null && (value == null || value == e.getElement())) {
                    // All entries following removed node can stay
                    // in list, but all preceding ones need to be
                    // cloned.
                    ++modCount;
                    HashEntry newFirst = e.next;
                    for (HashEntry p = first; p != e; p = p.next) {
                        newFirst = HashEntry.newHashEntry(p.key, p.hash, newFirst, p.getElement());
                    }
                    tab[index] = newFirst;
                    /*
                     * make sure we re-get from the HashEntry - since the decode in the conditional
                     * may have faulted in a different type - we must make sure we know what type
                     * to do the free on.
                     */
                    Object v = e.getElement();
                    Element toReturn = decode(v);

                    free(v);

                    long size;
                    if (v instanceof DiskStorageFactory.DiskMarker) {
                        size = onDiskPoolAccessor.delete(key, null, v);
                        LOG.debug("evicted {} from disk", size);
                    }
                    size = onHeapPoolAccessor.delete(key, v, HashEntry.newHashEntry(key, hash, null, null));
                    LOG.debug("evicted {} from heap", size);

                    // write-volatile
                    count = count - 1;
                    return toReturn;
                }

                return null;
            } finally {
                writeLock().unlock();
            }
        } else {
            return null;
        }
    }

    /**
     * Select a random sample of elements generated by the supplied factory.
     * 
     * @param filter filter of substitute types
     * @param sampleSize minimum number of elements to return
     * @param sampled collection in which to place the elements
     * @param seed random seed for the selection
     */
    void addRandomSample(ElementSubstituteFilter filter, int sampleSize, Collection<DiskStorageFactory.DiskSubstitute> sampled, int seed) {
        final HashEntry[] tab = table;
        final int tableStart = seed & (tab.length - 1);
        int tableIndex = tableStart;
        do {
            for (HashEntry e = tab[tableIndex]; e != null; e = e.next) {
                Object value = e.getElement();
                if (filter.allows(value)) {
                    sampled.add((DiskStorageFactory.DiskSubstitute) value);
                }
            }

            if (sampled.size() >= sampleSize) {
                return;
            }

            //move to next table slot
            tableIndex = (tableIndex + 1) & (tab.length - 1);
        } while (tableIndex != tableStart);
    }

    /**
     * Creates an iterator over the HashEntry objects within this Segment.
     * @return an iterator over the HashEntry objects within this Segment.
     */
    Iterator<HashEntry> hashIterator() {
        return new HashIterator();
    }

    @Override
    public String toString() {
        return super.toString() + " count: " + count;
    }

    /**
     * An iterator over the HashEntry objects within this Segment.
     */
    final class HashIterator implements Iterator<HashEntry> {
        private int nextTableIndex;
        private final HashEntry[] ourTable;
        private HashEntry nextEntry;
        private HashEntry lastReturned;

        private HashIterator() {
            if (count != 0) {
                ourTable = table;
                for (int j = ourTable.length - 1; j >= 0; --j) {
                    nextEntry = ourTable[j];
                    if (nextEntry != null) {
                        nextTableIndex = j - 1;
                        return;
                    }
                }
            } else {
                ourTable = null;
                nextTableIndex = -1;
            }
            advance();
        }

        private void advance() {
            if (nextEntry != null) {
                nextEntry = nextEntry.next;
                if (nextEntry != null) {
                    return;
                }
            }

            while (nextTableIndex >= 0) {
                nextEntry = ourTable[nextTableIndex--];
                if (nextEntry != null) {
                    return;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return nextEntry != null;
        }

        /**
         * {@inheritDoc}
         */
        public HashEntry next() {
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }
            lastReturned = nextEntry;
            advance();
            return lastReturned;
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            Segment.this.remove(lastReturned.key, lastReturned.hash, null, null);
            lastReturned = null;
        }
    }

    /**
     * Return the disk hit rate
     * @return the disk hit rate
     */
    public float getDiskHitRate() {
        return diskHitRate.getRate();
    }

    /**
     * Return the disk miss rate
     * @return the disk miss rate
     */
    public float getDiskMissRate() {
        return diskMissRate.getRate();
    }

    /**
     * Record a hit in the disk tier
     */
    protected void diskHit() {
        diskHitRate.event();
    }

    /**
     * Record a miss in the disk tier
     */
    protected void miss() {
        diskMissRate.event();
    }
}
