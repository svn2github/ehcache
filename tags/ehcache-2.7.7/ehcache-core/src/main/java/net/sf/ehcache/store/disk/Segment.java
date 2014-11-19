/**
 *  Copyright Terracotta, Inc.
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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.sf.ehcache.CacheOperationOutcomes.EvictionOutcome;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.disk.DiskStorageFactory.DiskMarker;
import net.sf.ehcache.store.disk.DiskStorageFactory.DiskSubstitute;
import net.sf.ehcache.store.disk.DiskStorageFactory.Placeholder;
import net.sf.ehcache.util.FindBugsSuppressWarnings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.observer.OperationObserver;

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
    private static final HashEntry NULL_HASH_ENTRY = new HashEntry(null, 0, null, null, new AtomicBoolean(false));

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

    private final PoolAccessor onHeapPoolAccessor;
    private final PoolAccessor onDiskPoolAccessor;
    private final RegisteredEventListeners cacheEventNotificationService;
    private volatile boolean cachePinned;

    private final OperationObserver<EvictionOutcome> evictionObserver;

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
     * @param cacheEventNotificationService
     */
    public Segment(int initialCapacity, float loadFactor, DiskStorageFactory primary,
                   CacheConfiguration cacheConfiguration,
                   PoolAccessor onHeapPoolAccessor, PoolAccessor onDiskPoolAccessor,
                   RegisteredEventListeners cacheEventNotificationService,
                   OperationObserver<EvictionOutcome> evictionObserver) {
        this.onHeapPoolAccessor = onHeapPoolAccessor;
        this.onDiskPoolAccessor = onDiskPoolAccessor;
        this.cacheEventNotificationService = cacheEventNotificationService;
        this.evictionObserver = evictionObserver;
        this.table = new HashEntry[initialCapacity];
        this.threshold = (int) (table.length * loadFactor);
        this.modCount = 0;
        this.disk = primary;
        this.cachePinned = determineCachePinned(cacheConfiguration);
    }

    private static boolean determineCachePinned(CacheConfiguration cacheConfiguration) {
        PinningConfiguration pinningConfiguration = cacheConfiguration.getPinningConfiguration();
        if (pinningConfiguration == null) {
            return false;
        }

        switch (pinningConfiguration.getStore()) {
            case LOCALMEMORY:
                return false;

            case INCACHE:
                return cacheConfiguration.isOverflowToDisk();

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
     *
     * @param key key to lookup
     * @param hash spread-hash for this key
     * @param markFaulted
     * @return mapped element
     */
    Element get(Object key, int hash, final boolean markFaulted) {
        readLock().lock();
        try {
            // read-volatile
            if (count != 0) {
                HashEntry e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key)) {
                        if (markFaulted) {
                            e.faulted.set(true);
                        }
                        return decodeHit(e.element);
                    }
                    e = e.next;
                }
            }
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
                        return e.element;
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
            if (e != null && comparator.equals(oldElement, decode(e.element))) {
                replaced = true;
                /*
                 * make sure we re-get from the HashEntry - since the decode in the conditional
                 * may have faulted in a different type - we must make sure we know what type
                 * to do the increment/decrement on.
                 */
                DiskSubstitute onDiskSubstitute = e.element;

                final long deltaHeapSize = onHeapPoolAccessor.replace(onDiskSubstitute.onHeapSize, key, encoded, NULL_HASH_ENTRY, cachePinned);
                if (deltaHeapSize == Long.MIN_VALUE) {
                    LOG.debug("replace3 failed to add on heap");
                    free(encoded);
                    return false;
                } else {
                    LOG.debug("replace3 added {} on heap", deltaHeapSize);
                    encoded.onHeapSize = onDiskSubstitute.onHeapSize + deltaHeapSize;
                }

                e.element = encoded;
                e.faulted.set(false);
                installed = true;
                free(onDiskSubstitute);

                if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                    final long outgoingDiskSize = onDiskPoolAccessor.delete(((DiskStorageFactory.DiskMarker) onDiskSubstitute).getSize());
                    LOG.debug("replace3 removed {} from disk", outgoingDiskSize);
                }
                cacheEventNotificationService.notifyElementUpdatedOrdered(oldElement, newElement);
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
                DiskSubstitute onDiskSubstitute = e.element;

                final long deltaHeapSize = onHeapPoolAccessor.replace(onDiskSubstitute.onHeapSize, key, encoded, NULL_HASH_ENTRY, cachePinned);
                if (deltaHeapSize == Long.MIN_VALUE) {
                    LOG.debug("replace2 failed to add on heap");
                    free(encoded);
                    return null;
                } else {
                    LOG.debug("replace2 added {} on heap", deltaHeapSize);
                    encoded.onHeapSize = onDiskSubstitute.onHeapSize + deltaHeapSize;
                }

                e.element = encoded;
                e.faulted.set(false);
                installed = true;
                oldElement = decode(onDiskSubstitute);
                free(onDiskSubstitute);

                if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                    final long outgoingDiskSize = onDiskPoolAccessor.delete(((DiskStorageFactory.DiskMarker) onDiskSubstitute).getSize());
                    LOG.debug("replace2 removed {} from disk", outgoingDiskSize);
                }
                cacheEventNotificationService.notifyElementUpdatedOrdered(oldElement, newElement);
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
    Element put(Object key, int hash, Element element, boolean onlyIfAbsent, boolean faulted) {
        boolean installed = false;
        DiskSubstitute encoded = disk.create(element);
        final long incomingHeapSize = onHeapPoolAccessor.add(key, encoded, NULL_HASH_ENTRY, cachePinned || faulted);
        if (incomingHeapSize < 0) {
            LOG.debug("put failed to add on heap");
            return null;
        } else {
            LOG.debug("put added {} on heap", incomingHeapSize);
            encoded.onHeapSize = incomingHeapSize;
        }

        writeLock().lock();
        try {
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
                DiskSubstitute onDiskSubstitute = e.element;
                if (!onlyIfAbsent) {
                    e.element = encoded;
                    installed = true;
                    oldElement = decode(onDiskSubstitute);

                    free(onDiskSubstitute);
                    final long existingHeapSize = onHeapPoolAccessor.delete(onDiskSubstitute.onHeapSize);
                    LOG.debug("put updated, deleted {} on heap", existingHeapSize);

                    if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                        final long existingDiskSize = onDiskPoolAccessor.delete(((DiskStorageFactory.DiskMarker) onDiskSubstitute).getSize());
                        LOG.debug("put updated, deleted {} on disk", existingDiskSize);
                    }
                    e.faulted.set(faulted);
                    cacheEventNotificationService.notifyElementUpdatedOrdered(oldElement, element);
                } else {
                    oldElement = decode(onDiskSubstitute);

                    free(encoded);
                    final long outgoingHeapSize = onHeapPoolAccessor.delete(encoded.onHeapSize);
                    LOG.debug("put if absent failed, deleted {} on heap", outgoingHeapSize);
                }
            } else {
                oldElement = null;
                ++modCount;
                tab[index] = new HashEntry(key, hash, first, encoded, new AtomicBoolean(faulted));
                installed = true;
                // write-volatile
                count = count + 1;
                cacheEventNotificationService.notifyElementPutOrdered(element);
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
    boolean putRawIfAbsent(Object key, int hash, DiskMarker encoded) throws IllegalArgumentException {
        writeLock().lock();
        try {
            if (!onDiskPoolAccessor.canAddWithoutEvicting(key, null, encoded)) {
                return false;
            }
            final long incomingHeapSize = onHeapPoolAccessor.add(key, encoded, NULL_HASH_ENTRY, cachePinned);
            if (incomingHeapSize < 0) {
                return false;
            } else {
                encoded.onHeapSize = incomingHeapSize;
            }
            if (onDiskPoolAccessor.add(key, null, encoded, cachePinned) < 0) {
                onHeapPoolAccessor.delete(encoded.onHeapSize);
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
                tab[index] = new HashEntry(key, hash, first, encoded, new AtomicBoolean(false));
                // write-volatile
                count = count + 1;
                return true;
            } else {
                onHeapPoolAccessor.delete(encoded.onHeapSize);
                onDiskPoolAccessor.delete(encoded.getSize());
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
                        newTable[k] = new HashEntry(p.key, p.hash, n, p.element, p.faulted);
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
                oldValue = decode(e.element);
                if (value == null || comparator.equals(value, oldValue)) {
                    // All entries following removed node can stay
                    // in list, but all preceding ones need to be
                    // cloned.
                    ++modCount;
                    HashEntry newFirst = e.next;
                    for (HashEntry p = first; p != e; p = p.next) {
                        newFirst = new HashEntry(p.key, p.hash, newFirst, p.element, p.faulted);
                    }
                    tab[index] = newFirst;
                    /*
                     * make sure we re-get from the HashEntry - since the decode in the conditional
                     * may have faulted in a different type - we must make sure we know what type
                     * to do the free on.
                     */
                    DiskSubstitute onDiskSubstitute = e.element;
                    free(onDiskSubstitute);

                    final long outgoingHeapSize = onHeapPoolAccessor.delete(onDiskSubstitute.onHeapSize);
                    LOG.debug("remove deleted {} from heap", outgoingHeapSize);

                    if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                        final long outgoingDiskSize = onDiskPoolAccessor.delete(((DiskStorageFactory.DiskMarker) onDiskSubstitute).getSize());
                        LOG.debug("remove deleted {} from disk", outgoingDiskSize);
                    }

                    cacheEventNotificationService.notifyElementRemovedOrdered(oldValue);

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
                        free(e.element);
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
    boolean fault(Object key, int hash, Placeholder expect, DiskMarker fault, final boolean skipFaulted) {
        writeLock().lock();
        try {
            return faultInternal(key, hash, expect, fault, skipFaulted);
        } finally {
            writeLock().unlock();
        }
    }

    // TODO Needs some serious clean up !
    private boolean faultInternal(final Object key, final int hash, final Placeholder expect, final DiskMarker fault, final boolean skipFaulted) {
        boolean faulted = cachePinned;
        if (count != 0 && !faulted) {
            HashEntry e = getFirst(hash);
            while (e != null) {
                if (e.hash == hash && key.equals(e.key)) {
                    faulted = e.faulted.get();
                }
                e = e.next;
            }

            if (skipFaulted && faulted) {
                free(fault, false);
                return true;
            }

            final long deltaHeapSize = onHeapPoolAccessor.replace(expect.onHeapSize, key, fault, NULL_HASH_ENTRY, faulted || cachePinned);
            if (deltaHeapSize == Long.MIN_VALUE) {
                remove(key, hash, null, null);
                return false;
            } else {
                fault.onHeapSize = expect.onHeapSize + deltaHeapSize;
                LOG.debug("fault removed {} from heap", deltaHeapSize);
            }
            final long incomingDiskSize = onDiskPoolAccessor.add(key, null, fault, faulted || cachePinned);
            if (incomingDiskSize < 0) {
                // replace must not fail here but it could if the memory freed by the previous replace has been stolen in the meantime
                // that's why it is forced, even if that could make the pool go over limit
                long deleteSize = onHeapPoolAccessor.replace(fault.onHeapSize, key, expect, NULL_HASH_ENTRY, true);
                LOG.debug("fault failed to add on disk, deleted {} from heap", deleteSize);
                expect.onHeapSize = fault.onHeapSize + deleteSize;
                final Element element = get(key, hash, false);
                return returnSafeDeprecated(key, hash, element);
            } else {
                LOG.debug("fault added {} on disk", incomingDiskSize);
            }

            if (findAndFree(key, hash, expect, fault)) {
                return true;
            }

            // replace must not fail here but it could if the memory freed by the previous replace has been stolen in the meantime
            // that's why it is forced, even if that could make the pool go over limit
            final long failDeltaHeapSize = onHeapPoolAccessor.replace(fault.onHeapSize, key, expect, NULL_HASH_ENTRY, true);
            LOG.debug("fault installation failed, deleted {} from heap", failDeltaHeapSize);
            expect.onHeapSize = fault.onHeapSize + failDeltaHeapSize;
            onDiskPoolAccessor.delete(incomingDiskSize);
            LOG.debug("fault installation failed deleted {} from disk", incomingDiskSize);
        }
        free(fault, true);
        return false;
    }

    private boolean findAndFree(final Object key, final int hash, final Placeholder expect, final DiskMarker fault) {
        for (HashEntry e = getFirst(hash); e != null; e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                if (expect == e.element) {
                    e.element = fault;
                    free(expect);
                    return true;
                }
            }
        }
        return false;
    }

    @Deprecated
    private boolean returnSafeDeprecated(final Object key, final int hash, final Element element) {
        notifyEviction(remove(key, hash, null, null));
        return false;
    }

    private void notifyEviction(final Element evicted) {
        if (evicted != null) {
            cacheEventNotificationService.notifyElementEvicted(evicted, false);
        }
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
    Element evict(Object key, int hash, DiskSubstitute value) {
        return evict(key, hash, value, true);
    }

    /**
     * Remove the matching mapping.  Unlike the {@link net.sf.ehcache.store.disk.Segment#remove(Object, int, net.sf.ehcache.Element, net.sf.ehcache.store.ElementValueComparator)} method
     * evict does referential comparison of the unretrieved substitute against the argument value.
     *
     * @param key key to match against
     * @param hash spread-hash for the key
     * @param value optional value to match against
     * @param notify whether to notify if we evict something
     * @return <code>true</code> on a successful remove
     */
    Element evict(Object key, int hash, DiskSubstitute value, boolean notify) {

        if (writeLock().tryLock()) {
            evictionObserver.begin();
            Element evictedElement = null;
            try {
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry first = tab[index];
                HashEntry e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key))) {
                    e = e.next;
                }

                if (e != null && !e.faulted.get()) {
                    evictedElement = decode(e.element);
                }

                // TODO this has to be removed!
                if (e != null && (value == null || value == e.element) && !e.faulted.get()) {
                    // All entries following removed node can stay
                    // in list, but all preceding ones need to be
                    // cloned.
                    ++modCount;
                    HashEntry newFirst = e.next;
                    for (HashEntry p = first; p != e; p = p.next) {
                        newFirst = new HashEntry(p.key, p.hash, newFirst, p.element, p.faulted);
                    }
                    tab[index] = newFirst;
                    /*
                     * make sure we re-get from the HashEntry - since the decode in the conditional
                     * may have faulted in a different type - we must make sure we know what type
                     * to do the free on.
                     */
                    DiskSubstitute onDiskSubstitute = e.element;
                    free(onDiskSubstitute);

                    final long outgoingHeapSize = onHeapPoolAccessor.delete(onDiskSubstitute.onHeapSize);
                    LOG.debug("evicted {} from heap", outgoingHeapSize);

                    if (onDiskSubstitute instanceof DiskStorageFactory.DiskMarker) {
                        final long outgoingDiskSize = onDiskPoolAccessor.delete(((DiskStorageFactory.DiskMarker) onDiskSubstitute).getSize());
                        LOG.debug("evicted {} from disk", outgoingDiskSize);
                    }

                    if (notify) {
                        cacheEventNotificationService.notifyElementRemovedOrdered(evictedElement);
                    }

                    // write-volatile
                    count = count - 1;
                } else {
                    evictedElement = null;
                }
                return evictedElement;
            } finally {
                writeLock().unlock();
                if (notify && evictedElement != null) {
                    if (evictedElement.isExpired()) {
                        cacheEventNotificationService.notifyElementExpiry(evictedElement, false);
                    } else {
                        evictionObserver.end(EvictionOutcome.SUCCESS);
                        cacheEventNotificationService.notifyElementEvicted(evictedElement, false);
                    }
                }
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
        if (count == 0) {
            return;
        }
        final HashEntry[] tab = table;
        final int tableStart = seed & (tab.length - 1);
        int tableIndex = tableStart;
        do {
            for (HashEntry e = tab[tableIndex]; e != null; e = e.next) {
                Object value = e.element;
                if (!e.faulted.get() && filter.allows(value)) {
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
     * Will check whether a Placeholder that failed to flush to disk is lying around
     * If so, it'll try to evict it
     * @param key the key
     * @param hash the key's hash
     * @return true if a failed marker was or is still there, false otherwise
     */
    @FindBugsSuppressWarnings("UL_UNRELEASED_LOCK")
    boolean cleanUpFailedMarker(final Serializable key, final int hash) {
        boolean readLocked = false;
        boolean failedMarker = false;
        if (!isWriteLockedByCurrentThread()) {
            readLock().lock();
            readLocked = true;
        }
        DiskSubstitute substitute = null;
        try {
            if (count != 0) {
                HashEntry e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key)) {
                        substitute = e.element;
                        if (substitute instanceof Placeholder) {
                            failedMarker = ((Placeholder)substitute).hasFailedToFlush();
                            break;
                        }
                    }
                    e = e.next;
                }
            }
        } finally {
            if (readLocked) {
                readLock().unlock();
            }
        }
        if (failedMarker) {
            evict(key, hash, substitute, false);
        }
        return failedMarker;
    }

    /**
     * Marks an entry as flushable to disk (i.e. not faulted in higher tiers)
     * Also updates the access stats
     * @param key the key
     * @param hash they hash
     * @param element the expected element
     * @return true if succeeded
     */
    boolean flush(final Object key, final int hash, final Element element) {
        DiskSubstitute diskSubstitute = null;
        readLock().lock();
        try {
            HashEntry e = getFirst(hash);
            while (e != null) {
                if (e.hash == hash && key.equals(e.key)) {
                    final boolean b = e.faulted.compareAndSet(true, false);
                    diskSubstitute = e.element;
                    if (diskSubstitute instanceof Placeholder) {
                        if (((Placeholder)diskSubstitute).hasFailedToFlush() && evict(key, hash, diskSubstitute) != null) {
                            diskSubstitute = null;
                        }
                    } else {
                        if (diskSubstitute instanceof DiskMarker) {
                            final DiskMarker diskMarker = (DiskMarker)diskSubstitute;
                            diskMarker.updateStats(element);
                        }
                    }
                    return b;
                }
                e = e.next;
            }
        } finally {
            readLock().unlock();
            if (diskSubstitute != null && element.isExpired()) {
                evict(key, hash, diskSubstitute);
            }
        }
        return false;
    }

    /**
     * Clears the faulted but on all entries
     */
    void clearFaultedBit() {
        writeLock().lock();
        try {
            HashEntry entry;
            for (HashEntry hashEntry : table) {
                entry = hashEntry;
                while (entry != null) {
                    entry.faulted.set(false);
                    entry = entry.next;
                }
            }
        } finally {
            writeLock().unlock();
        }
    }

    /**
     * Verifies if the mapping for a key is marked as faulted
     * @param key the key to check the mapping for
     * @return true if faulted, false otherwise (including no mapping)
     */
    public boolean isFaulted(final int hash, final Object key) {
        readLock().lock();
        try {
            // read-volatile
            if (count != 0) {
                HashEntry e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.key)) {
                        return e.faulted.get();
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
}
