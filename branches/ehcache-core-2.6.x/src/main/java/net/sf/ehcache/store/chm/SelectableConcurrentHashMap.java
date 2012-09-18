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
package net.sf.ehcache.store.chm;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.Element;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;

/**
 * SelectableConcurrentHashMap subclasses a repackaged version of ConcurrentHashMap
 * ito allow efficient random sampling of the map values.
 * <p>
 * The random sampling technique involves randomly selecting a map Segment, and then
 * selecting a number of random entry chains from that segment.
 *
 * @author Chris Dennis
 */
public class SelectableConcurrentHashMap {

    protected static final Element DUMMY_PINNED_ELEMENT = new Element(new DummyPinnedKey(), new DummyPinnedValue());

    /**
     * The maximum capacity, used if a higher value is implicitly
     * specified by either of the constructors with arguments.  MUST
     * be a power of two <= 1<<30 to ensure that entries are indexable
     * using ints.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The maximum number of segments to allow; used to bound
     * constructor arguments.
     */
    private static final int MAX_SEGMENTS = 1 << 16; // slightly conservative

    /**
     * Number of unsynchronized retries in size and containsValue
     * methods before resorting to locking. This is used to avoid
     * unbounded retries if tables undergo continuous modification
     * which would make it impossible to obtain an accurate result.
     */
    private static final int RETRIES_BEFORE_LOCK = 2;

    /**
     * Mask value for indexing into segments. The upper bits of a
     * key's hash code are used to choose the segment.
     */
    private final int segmentMask;

    /**
     * Shift value for indexing within segments.
     */
    private final int segmentShift;

    /**
     * The segments, each of which is a specialized hash table
     */
    private final Segment[] segments;

    private final Random rndm = new Random();
    private final PoolAccessor poolAccessor;
    private final boolean elementPinningEnabled;
    private volatile long maxSize;
    private volatile SelectableConcurrentHashMap.PinnedKeySet pinnedKeySet;
    private final RegisteredEventListeners cacheEventNotificationService;

    private Set<Object> keySet;
    private Set<Map.Entry<Object,Element>> entrySet;
    private Collection<Element> values;

    public SelectableConcurrentHashMap(PoolAccessor poolAccessor, boolean elementPinningEnabled, int initialCapacity, float loadFactor, int concurrency, final long maximumSize, final RegisteredEventListeners cacheEventNotificationService) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrency <= 0)
            throw new IllegalArgumentException();

        if (concurrency > MAX_SEGMENTS)
            concurrency = MAX_SEGMENTS;

        // Find power-of-two sizes best matching arguments
        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrency) {
            ++sshift;
            ssize <<= 1;
        }
        segmentShift = 32 - sshift;
        segmentMask = ssize - 1;
        this.segments = new Segment[ssize];

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        int c = initialCapacity / ssize;
        if (c * ssize < initialCapacity)
            ++c;
        int cap = 1;
        while (cap < c)
            cap <<= 1;

        for (int i = 0; i < this.segments.length; ++i)
            this.segments[i] = createSegment(cap, loadFactor);

        this.poolAccessor = poolAccessor;
        this.elementPinningEnabled = elementPinningEnabled;
        this.maxSize = maximumSize;
        this.cacheEventNotificationService = cacheEventNotificationService;
        pinnedKeySet = new PinnedKeySet();
    }

    public void setMaxSize(final long maxSize) {
        this.maxSize = maxSize;
    }

    public Element[] getRandomValues(final int size, Object keyHint) {
        ArrayList<Element> sampled = new ArrayList<Element>(size * 2);

        // pick a random starting point in the map
        int randomHash = rndm.nextInt();

        final int segmentStart;
        if (keyHint == null) {
            segmentStart = (randomHash >>> segmentShift) & segmentMask;
        } else {
            segmentStart = (hash(keyHint.hashCode()) >>> segmentShift) & segmentMask;
        }

        int segmentIndex = segmentStart;
        do {
            final HashEntry[] table = segments[segmentIndex].table;
            final int tableStart = randomHash & (table.length - 1);
            int tableIndex = tableStart;
            do {
                for (HashEntry e = table[tableIndex]; e != null; e = e.next) {
                    Element value = e.value;
                    if (value != null && (value.isExpired() || !(e.pinned && elementPinningEnabled))) {
                        sampled.add(value);
                    }
                }

                if (sampled.size() >= size) {
                    return sampled.toArray(new Element[sampled.size()]);
                }

                //move to next table slot
                tableIndex = (tableIndex + 1) & (table.length - 1);
            } while (tableIndex != tableStart);

            //move to next segment
            segmentIndex = (segmentIndex + 1) & segmentMask;
        } while (segmentIndex != segmentStart);

        return sampled.toArray(new Element[sampled.size()]);
    }

    /**
     * Return an object of the kind which will be stored when
     * the element is going to be inserted
     * @param e the element
     * @return an object looking-alike the stored one
     */
    public Object storedObject(Element e) {
        return new HashEntry(null, 0, null, e, 0, false);
    }

    /**
     * Returns the number of key-value mappings in this map without locking anything.
     * This may not give the exact element count as locking is avoided.
     * If the map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    public int quickSize() {
        final Segment[] segments = this.segments;
        long sum = 0;
        for (Segment seg : segments) {
            sum += seg.count - seg.numDummyPinnedKeys;
        }

        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int)sum;
        }
    }

    public boolean isEmpty() {
        final Segment[] segments = this.segments;
        /*
         * We keep track of per-segment modCounts to avoid ABA
         * problems in which an element in one segment was added and
         * in another removed during traversal, in which case the
         * table was never actually empty at any point. Note the
         * similar use of modCounts in the size() and containsValue()
         * methods, which are the only other methods also susceptible
         * to ABA problems.
         */
        int[] mc = new int[segments.length];
        int mcsum = 0;
        for (int i = 0; i < segments.length; ++i) {
            if (segments[i].count != 0)
                return false;
            else
                mcsum += mc[i] = segments[i].modCount;
        }
        // If mcsum happens to be zero, then we know we got a snapshot
        // before any modifications at all were made.  This is
        // probably common enough to bother tracking.
        if (mcsum != 0) {
            for (int i = 0; i < segments.length; ++i) {
                if (segments[i].count != 0 ||
                    mc[i] != segments[i].modCount)
                    return false;
            }
        }
        return true;
    }

    public int size() {
        final Segment[] segments = this.segments;

        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            int[] mc = new int[segments.length];
            long check = 0;
            long sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].count - segments[i].numDummyPinnedKeys;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    check += segments[i].count - segments[i].numDummyPinnedKeys;
                    if (mc[i] != segments[i].modCount) {
                        check = -1; // force retry
                        break;
                    }
                }
            }
            if (check == sum) {
                if (sum > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                } else {
                    return (int)sum;
                }
            }
        }

        long sum = 0;
        for (int i = 0; i < segments.length; ++i) {
            segments[i].readLock().lock();
        }
        try {
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].count - segments[i].numDummyPinnedKeys;
            }
        } finally {
            for (int i = 0; i < segments.length; ++i) {
                segments[i].readLock().unlock();
            }
        }

        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int)sum;
        }
    }

    public int pinnedSize() {
        final Segment[] segments = this.segments;

        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            int[] mc = new int[segments.length];
            long check = 0;
            long sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].pinnedCount - segments[i].numDummyPinnedKeys;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    check += segments[i].pinnedCount - segments[i].numDummyPinnedKeys;
                    if (mc[i] != segments[i].modCount) {
                        check = -1; // force retry
                        break;
                    }
                }
            }
            if (check == sum) {
                if (sum > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                } else {
                    return (int)sum;
                }
            }
        }

        long sum = 0;
        for (int i = 0; i < segments.length; ++i) {
            segments[i].readLock().lock();
        }
        try {
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].pinnedCount - segments[i].numDummyPinnedKeys;
            }
        } finally {
            for (int i = 0; i < segments.length; ++i) {
                segments[i].readLock().unlock();
            }
        }

        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int)sum;
        }
    }

    public ReentrantReadWriteLock lockFor(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash);
    }

    public ReentrantReadWriteLock[] locks() {
        return segments;
    }

    public Element get(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).get(key, hash);
    }

    public boolean containsKey(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).containsKey(key, hash);
    }

    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();

        // See explanation of modCount use above

        final Segment[] segments = this.segments;
        int[] mc = new int[segments.length];

        // Try a few times without locking
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            int sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                int c = segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
                if (segments[i].containsValue(value))
                    return true;
            }
            boolean cleanSweep = true;
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    int c = segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        cleanSweep = false;
                        break;
                    }
                }
            }
            if (cleanSweep)
                return false;
        }

        // Resort to locking all segments
        for (int i = 0; i < segments.length; ++i)
            segments[i].readLock().lock();
        try {
            for (int i = 0; i < segments.length; ++i) {
                if (segments[i].containsValue(value)) {
                    return true;
                }
            }
        } finally {
            for (int i = 0; i < segments.length; ++i)
                segments[i].readLock().unlock();
        }
        return false;
    }

    public Element put(Object key, Element element, long sizeOf) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, element, sizeOf, false, false, true);
    }

    public Element putIfAbsent(Object key, Element element, long sizeOf) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, element, sizeOf, true, false, true);
    }

    public Element remove(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).remove(key, hash, null);
    }

    public boolean remove(Object key, Object value) {
        int hash = hash(key.hashCode());
        if (value == null)
            return false;
        return segmentFor(hash).remove(key, hash, value) != null;
    }

    public void clear() {
        for (int i = 0; i < segments.length; ++i)
            segments[i].clear();
    }

    public void unpinAll() {
        for (Segment segment : this.segments) {
            segment.unpinAll();
        }
    }

    public void setPinned(Object key, boolean pinned) {
        int hash = hash(key.hashCode());
        segmentFor(hash).setPinned(key, pinned, hash);
    }

    public boolean isPinned(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).isPinned(key, hash);
    }

    public Set<Object> keySet() {
        Set<Object> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet());
    }

    public Collection<Element> values() {
        Collection<Element> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    public Set<Entry<Object, Element>> entrySet() {
        Set<Entry<Object, Element>> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    protected Segment createSegment(int initialCapacity, float lf) {
        return new Segment(initialCapacity, lf);
    }

    public boolean evict() {
        return getRandomSegment().evict();
    }

    private Segment getRandomSegment() {
        int randomHash = rndm.nextInt();
        return segments[((randomHash >>> segmentShift) & segmentMask)];
    }

    public void recalculateSize(Object key) {
        int hash = hash(key.hashCode());
        segmentFor(hash).recalculateSize(key, hash);
    }

    public Set pinnedKeySet() {
        Set<Object> pks = pinnedKeySet;
        return (pks != null) ? pks : (pinnedKeySet = new PinnedKeySet());
    }

    /**
     * Returns the segment that should be used for key with given hash
     * @param hash the hash code for the key
     * @return the segment
     */
    protected final Segment segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    protected final List<Segment> segments() {
        return Collections.unmodifiableList(Arrays.asList(segments));
    }

    public class Segment extends ReentrantReadWriteLock {

        private static final int MAX_EVICTION = 5;

        /**
         * The number of elements in this segment's region.
         */
        protected volatile int count;

        /**
         * Number of updates that alter the size of the table. This is
         * used during bulk-read methods to make sure they see a
         * consistent snapshot: If modCounts change during a traversal
         * of segments computing size or checking containsValue, then
         * we might have an inconsistent view of state so (usually)
         * must retry.
         */
        int modCount;

        /**
         * The table is rehashed when its size exceeds this threshold.
         * (The value of this field is always <tt>(int)(capacity *
         * loadFactor)</tt>.)
         */
        int threshold;

        /**
         * The per-segment table.
         */
        protected volatile HashEntry[] table;

        /**
         * The load factor for the hash table.  Even though this value
         * is same for all segments, it is replicated to avoid needing
         * links to outer object.
         * @serial
         */
        final float loadFactor;

        private Iterator<HashEntry> evictionIterator = iterator();
        private boolean fullyPinned;
        protected volatile int pinnedCount;
        protected volatile int numDummyPinnedKeys;

        protected Segment(int initialCapacity, float lf) {
            loadFactor = lf;
            setTable(new HashEntry[initialCapacity]);
        }

        protected void preRemove(HashEntry e) {

        }

        protected void postInstall(Object key, Element value, boolean pinned) {

        }

        /**
         * Sets table to new HashEntry array.
         * Call only while holding lock or in constructor.
         */
        void setTable(HashEntry[] newTable) {
            threshold = (int)(newTable.length * loadFactor);
            table = newTable;
        }

        /**
         * Returns properly casted first entry of bin for given hash.
         */
        protected HashEntry getFirst(int hash) {
            HashEntry[] tab = table;
            return tab[hash & (tab.length - 1)];
        }

        public void setPinned(Object key, boolean pinned, int hash) {
            writeLock().lock();
            try {
                HashEntry e = getFirst(hash);
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;
                if (e != null) {
                    if (pinned && !e.pinned) {
                        pinnedCount++;
                        e.pinned = true;
                        postInstall(e.key, e.value, true);
                    } else if (!pinned && e.pinned) {
                        pinnedCount--;
                        if(!e.checkAndAssertDummyPinnedEntry()) {
                            e.pinned = false;
                        } else {
                            HashEntry[] tab = table;
                            int index = hash & (tab.length - 1);
                            HashEntry first = tab[index];
                            tab[index] = removeAndGetFirst(e, first);
                            --count;
                            --numDummyPinnedKeys;
                            ++modCount;
                        }
                        postInstall(e.key, e.value, false);
                    }
                } else if (pinned) {
                    put(key, hash, DUMMY_PINNED_ELEMENT, 0, false, true, true);
                    pinnedCount++;
                    numDummyPinnedKeys++;
                }
            } finally {
                writeLock().unlock();
            }
        }

        private HashEntry removeAndGetFirst(HashEntry e, HashEntry first) {
            preRemove(e);
            // All entries following removed node can stay
            // in list, but all preceding ones need to be
            // cloned.
            HashEntry newFirst = e.next;
            for (HashEntry p = first; p != e; p = p.next)
                newFirst = relinkHashEntry(p, newFirst);
            return newFirst;
        }

        public boolean isPinned(Object key, int hash) {
            readLock().lock();
            try {
                HashEntry e = getFirst(hash);
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;
                if (e != null) {
                    return e.pinned;
                }
                return false;
            } finally {
                readLock().unlock();
            }
        }

        public void unpinAll() {
            writeLock().lock();
            try {
                if(numDummyPinnedKeys == count) {
                    clear();
                    return;
                }

                // using clock iterator here so maintaining number of visited entries
                int numVisited = 0;
                int dummyPinnedKeys = 0;
                for(int i=0; i < table.length && numVisited < count; ++i) {
                    HashEntry newFirst = null;
                    HashEntry current = table[i];
                    while(current != null && numVisited < count) {
                        if(!current.checkAndAssertDummyPinnedEntry()) {
                            current.pinned = false;
                            newFirst = newFirst == null ? current : relinkHashEntry(current, newFirst);
                        } else {
                            preRemove(current);
                            ++dummyPinnedKeys;
                        }
                        ++numVisited;
                        current = current.next;
                    }
                    table[i] = newFirst;
                }
                if(numDummyPinnedKeys != dummyPinnedKeys) {
                    throw new IllegalStateException("numDummyPinnedKeys "+numDummyPinnedKeys+" but dummyPinnedKeys"+dummyPinnedKeys);
                }
                if(dummyPinnedKeys > 0) {
                    count -= dummyPinnedKeys;
                    ++modCount;
                }
                pinnedCount = numDummyPinnedKeys = 0;
            } finally {
                writeLock().unlock();
            }
        }

        protected HashEntry createHashEntry(Object key, int hash, HashEntry next, Element value, long sizeOf, boolean pinned) {
            return new HashEntry(key, hash, next, value, sizeOf, pinned);
        }

        protected HashEntry relinkHashEntry(HashEntry e, HashEntry next) {
            return new HashEntry(e.key, e.hash, next, e.value, e.sizeOf, e.pinned);
        }

        protected void clear() {
            writeLock().lock();
            try {
                if (count != 0) {
                    HashEntry[] tab = table;
                    for (int i = 0; i < tab.length ; i++)
                        tab[i] = null;
                    ++modCount;
                    numDummyPinnedKeys = 0;
                    pinnedCount = 0;
                    count = 0; // write-volatile
                }
            } finally {
                writeLock().unlock();
            }
        }

        Element remove(Object key, int hash, Object value) {
            writeLock().lock();
            try {
                int c = count - 1;
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry first = tab[index];
                HashEntry e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                Element oldValue = null;
                if (e != null) {
                    Element v = e.value;
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        ++modCount;
                        if(!e.pinned) {
                            tab[index] = removeAndGetFirst(e, first);
                        } else {
                            ++c;
                            if (oldValue == DUMMY_PINNED_ELEMENT) {
                               oldValue = null;
                            } else {
                                preRemove(e);
                                e.value = DUMMY_PINNED_ELEMENT;
                                ++numDummyPinnedKeys;
                            }
                        }
                        count = c; // write-volatile
                        poolAccessor.delete(e.sizeOf);
                    }
                }
                return oldValue;
            } finally {
                writeLock().unlock();
            }
        }

        public void recalculateSize(Object key, int hash) {
            Element value = null;
            long oldSize = 0;
            readLock().lock();
            try {
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry first = tab[index];
                HashEntry e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key))) {
                    e = e.next;
                }
                if (e != null) {
                    key = e.key;
                    value = e.value;
                    oldSize = e.sizeOf;
                }
            } finally {
                readLock().unlock();
            }
            if (value != null) {
                long delta = poolAccessor.replace(oldSize, key, value, storedObject(value), true);
                writeLock().lock();
                try {
                    HashEntry e = getFirst(hash);
                    while (e != null && key != e.key) {
                        e = e.next;
                    }

                    if (e != null && e.value == value && oldSize == e.sizeOf) {
                        e.sizeOf = oldSize + delta;
                    } else {
                        poolAccessor.delete(delta);
                    }
                } finally {
                    writeLock().unlock();
                }
            }
        }

        protected Element put(Object key, int hash, Element value, long sizeOf, boolean onlyIfAbsent, boolean pinned, boolean fire) {
            Element[] evicted = new Element[MAX_EVICTION];
            writeLock().lock();
            try {
                int c = count;
                if (c++ > threshold) // ensure capacity
                    rehash();
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry first = tab[index];
                HashEntry e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                Element oldValue;
                if (e != null) {
                    oldValue = e.value;
                    if (e.value == DUMMY_PINNED_ELEMENT || !onlyIfAbsent) {
                        poolAccessor.delete(e.sizeOf);
                        e.value = value;
                        e.sizeOf = sizeOf;
                        if (oldValue == DUMMY_PINNED_ELEMENT && value != DUMMY_PINNED_ELEMENT) {
                            --numDummyPinnedKeys;
                            oldValue = null;
                        }
                        if (fire) {
                            postInstall(key, value, e.pinned);
                        }
                    }
                } else {
                    oldValue = null;
                    ++modCount;
                    tab[index] = createHashEntry(key, hash, first, value, sizeOf, pinned);
                    count = c; // write-volatile
                    if (fire) {
                        postInstall(key, value, pinned);
                    }
                }

                if(!pinned && (onlyIfAbsent && oldValue != null || !onlyIfAbsent)) {
                    if (!isPinned(key, hash)) {
                        this.fullyPinned = false;
                    }
                    if (!fullyPinned && SelectableConcurrentHashMap.this.maxSize > 0) {
                        int runs = Math.min(MAX_EVICTION, SelectableConcurrentHashMap.this.quickSize() - (int) SelectableConcurrentHashMap.this.maxSize);
                        while (runs-- > 0) {
                            Element evict = nextExpiredOrToEvict(value);
                            if (evict != null) {
                                Element removed;
                                while ((removed = remove(evict.getKey(), hash(evict.getKey().hashCode()), null)) == null) {
                                    evict = nextExpiredOrToEvict(value);
                                    if (evict == null) {
                                        break;
                                    }
                                }
                                evicted[runs] = removed;
                            }
                        }
                    }
                }
                return oldValue;
            } finally {
                writeLock().unlock();
                for (Element element : evicted) {
                    notifyEvictionOrExpiry(element);
                }
            }
        }

        private void notifyEvictionOrExpiry(final Element element) {
            if(element != null && cacheEventNotificationService != null) {
                if (element.isExpired()) {
                    cacheEventNotificationService.notifyElementExpiry(element, false);
                } else {
                    cacheEventNotificationService.notifyElementEvicted(element, false);
                }
            }
        }

        Element get(final Object key, final int hash) {
            readLock().lock();
            try {
                if (count != 0) { // read-volatile
                    HashEntry e = getFirst(hash);
                    while (e != null) {
                        if (e.hash == hash && key.equals(e.key) && !e.value.equals(DUMMY_PINNED_ELEMENT)) {
                            e.accessed = true;
                            return e.value;
                        }
                        e = e.next;
                    }
                }
                return null;
            } finally {
                readLock().unlock();
            }
        }

        boolean containsKey(final Object key, final int hash) {
            readLock().lock();
            try {
                if (count != 0) { // read-volatile
                    HashEntry e = getFirst(hash);
                    while (e != null) {
                        if (e.hash == hash && key.equals(e.key) && !e.value.equals(DUMMY_PINNED_ELEMENT))
                            return true;
                        e = e.next;
                    }
                }
                return false;
            } finally {
                readLock().unlock();
            }
        }

        boolean containsValue(Object value) {
            readLock().lock();
            try {
                if (count != 0) { // read-volatile
                    HashEntry[] tab = table;
                    int len = tab.length;
                    for (int i = 0 ; i < len; i++) {
                        for (HashEntry e = tab[i]; e != null; e = e.next) {
                            Element v = e.value;
                            if (value.equals(v))
                                return true;
                        }
                    }
                }
                return false;
            } finally {
                readLock().unlock();
            }
        }

        private Element nextExpiredOrToEvict(final Element justAdded) {

            Element lastUnpinned = null;
            int i = 0;

            while (!fullyPinned && i++ < count) {
                if (!evictionIterator.hasNext()) {
                    evictionIterator = iterator();
                }
                final HashEntry next = evictionIterator.next();
                if (next.value.isExpired() || !next.accessed) {
                    return next.value;
                } else {
                    final boolean pinned = next.pinned;
                    if (!pinned && next.value != justAdded) {
                        lastUnpinned = next.value;
                    }
                    next.accessed = pinned;
                }
            }

            this.fullyPinned = !this.fullyPinned && i >= count && lastUnpinned == null;

            return lastUnpinned;
        }

        protected Iterator<HashEntry> iterator() {
            return new SegmentIterator(this);
        }

        private boolean evict() {
            Element remove = null;
            writeLock().lock();
            try {
                Element evict = nextExpiredOrToEvict(null);
                if (evict != null) {
                    remove = remove(evict.getKey(), hash(evict.getKey().hashCode()), null);
                }
            } finally {
                writeLock().unlock();
            }
            notifyEvictionOrExpiry(remove);
            return remove != null;
        }

        void rehash() {
            HashEntry[] oldTable = table;
            int oldCapacity = oldTable.length;
            if (oldCapacity >= MAXIMUM_CAPACITY)
                return;

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
            threshold = (int)(newTable.length * loadFactor);
            int sizeMask = newTable.length - 1;
            for (int i = 0; i < oldCapacity ; i++) {
                // We need to guarantee that any existing reads of old Map can
                //  proceed. So we cannot yet null out each bin.
                HashEntry e = oldTable[i];

                if (e != null) {
                    HashEntry next = e.next;
                    int idx = e.hash & sizeMask;

                    //  Single node on list
                    if (next == null)
                        newTable[idx] = e;

                    else {
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
                            newTable[k] = relinkHashEntry(p, n);
                        }
                    }
                }
            }
            table = newTable;
        }
    }

    public static class HashEntry {
        public final Object key;
        public final int hash;
        public final HashEntry next;

        public volatile Element value;

        public volatile boolean pinned;
        public volatile long sizeOf;
        public volatile boolean accessed = true;

        protected HashEntry(Object key, int hash, HashEntry next, Element value, long sizeOf, boolean pinned) {
            this.key = key;
            this.hash = hash;
            this.next = next;
            this.value = value;
            this.sizeOf = sizeOf;
            this.pinned = pinned;
        }

        boolean checkAndAssertDummyPinnedEntry() {
            if(value == DUMMY_PINNED_ELEMENT && !pinned) {
                throw new IllegalStateException("HashEntry value is DUMMY_PINNED_ELEMENT but pinned "+pinned);
            }
            return value == DUMMY_PINNED_ELEMENT;
        }
    }

    private class SegmentIterator implements Iterator<HashEntry> {

        int nextTableIndex;
        HashEntry[] currentTable;
        HashEntry nextEntry;
        HashEntry lastReturned;
        private final Segment seg;

        private SegmentIterator(final Segment memoryStoreSegment) {
            nextTableIndex = -1;
            this.seg = memoryStoreSegment;
            advance();
        }

        public boolean hasNext() {
            return nextEntry != null;
        }

        public HashEntry next() {
            if (nextEntry == null)
                return null;
            lastReturned = nextEntry;
            advance();
            return lastReturned;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }

        final void advance() {
            if (nextEntry != null && (nextEntry = nextEntry.next) != null)
                return;
            while (nextTableIndex >= 0) {
                if ( (nextEntry = currentTable[nextTableIndex--]) != null)
                    return;
            }
            if (seg.count != 0) {
                currentTable = seg.table;
                for (int j = currentTable.length - 1; j >= 0; --j) {
                    if ( (nextEntry = currentTable[j]) != null) {
                        nextTableIndex = j - 1;
                        return;
                    }
                }
            }
        }
    }

    @IgnoreSizeOf
    protected static class DummyPinnedKey implements Serializable {

    }

    @IgnoreSizeOf
    protected static class DummyPinnedValue implements Serializable {

    }

    final class KeySet extends AbstractSet<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new KeyIterator();
        }

        @Override
        public int size() {
            return SelectableConcurrentHashMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return SelectableConcurrentHashMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return SelectableConcurrentHashMap.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return SelectableConcurrentHashMap.this.remove(o) != null;
        }

        @Override
        public void clear() {
            SelectableConcurrentHashMap.this.clear();
        }

        @Override
        public Object[] toArray() {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray();
        }
        @Override
        public <T> T[] toArray(T[] a) {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray(a);
        }
    }

    final class Values extends AbstractCollection<Element> {

        @Override
        public Iterator<Element> iterator() {
            return new ValueIterator();
        }

        @Override
        public int size() {
            return SelectableConcurrentHashMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return SelectableConcurrentHashMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return SelectableConcurrentHashMap.this.containsValue(o);
        }

        @Override
        public void clear() {
            SelectableConcurrentHashMap.this.clear();
        }

        @Override
        public Object[] toArray() {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray(a);
        }
    }

    final class EntrySet extends AbstractSet<Entry<Object, Element>> {

        @Override
        public Iterator<Entry<Object, Element>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return SelectableConcurrentHashMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return SelectableConcurrentHashMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            Element v = SelectableConcurrentHashMap.this.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            return SelectableConcurrentHashMap.this.remove(e.getKey(), e.getValue());
        }

        @Override
        public void clear() {
            SelectableConcurrentHashMap.this.clear();
        }

        @Override
        public Object[] toArray() {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray();
        }
        @Override
        public <T> T[] toArray(T[] a) {
            Collection<Object> c = new ArrayList<Object>();
            for (Object object : this)
                c.add(object);
            return c.toArray(a);
        }
    }

    class KeyIterator extends HashEntryIterator implements Iterator<Object> {

        @Override
        public Object next() {
            return nextEntry().key;
        }
    }

    final class ValueIterator extends HashEntryIterator implements Iterator<Element> {

        @Override
        public Element next() {
            return nextEntry().value;
        }
    }

    final class EntryIterator extends HashEntryIterator implements Iterator<Entry<Object, Element>> {

        @Override
        public Entry<Object, Element> next() {
            HashEntry entry = nextEntry();
            final Object key = entry.key;
            final Element value = entry.value;
            return new Entry<Object, Element>() {

                public Object getKey() {
                    return key;
                }

                public Element getValue() {
                    return value;
                }

                public Element setValue(Element value) {
                  throw new UnsupportedOperationException();
                }
            };
        }
    }

    abstract class HashEntryIterator extends HashIterator {
        private HashEntry myNextEntry;

        public HashEntryIterator() {
            myNextEntry = advanceToNextEntry();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }

        @Override
        public HashEntry nextEntry() {
            if (myNextEntry == null) {
                throw new NoSuchElementException();
            }
            HashEntry entry = myNextEntry;
            myNextEntry = advanceToNextEntry();
            return entry;
        }

        @Override
        public boolean hasNext() {
            return myNextEntry != null;
        }

        private HashEntry advanceToNextEntry() {
            HashEntry myEntry = null;
            while (super.hasNext()) {
                myEntry = super.nextEntry();
                if (myEntry != null && !hideValue(myEntry)) {
                    break;
                } else {
                    myEntry = null;
                }
            }
            return myEntry;
        }

        protected boolean hideValue(HashEntry hashEntry) {
            return hashEntry.value == DUMMY_PINNED_ELEMENT;
        }
    }

    abstract class HashIterator {
        int nextSegmentIndex;
        int nextTableIndex;
        HashEntry[] currentTable;
        HashEntry nextEntry;
        HashEntry lastReturned;

        HashIterator() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            advance();
        }

        public boolean hasMoreElements() { return hasNext(); }

        final void advance() {
            if (nextEntry != null && (nextEntry = nextEntry.next) != null)
                return;

            while (nextTableIndex >= 0) {
                if ( (nextEntry = currentTable[nextTableIndex--]) != null)
                    return;
            }

            while (nextSegmentIndex >= 0) {
                Segment seg = segments[nextSegmentIndex--];
                if (seg.count != 0) {
                    currentTable = seg.table;
                    for (int j = currentTable.length - 1; j >= 0; --j) {
                        if ( (nextEntry = currentTable[j]) != null) {
                            nextTableIndex = j - 1;
                            return;
                        }
                    }
                }
            }
        }

        public boolean hasNext() { return nextEntry != null; }

        HashEntry nextEntry() {
            if (nextEntry == null)
                throw new NoSuchElementException();
            lastReturned = nextEntry;
            advance();
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            SelectableConcurrentHashMap.this.remove(lastReturned.key);
            lastReturned = null;
        }
    }

    private class PinnedKeySet extends AbstractSet<Object> {
        @Override
        public Iterator<Object> iterator() {
            return new PinnedKeyIterator();
        }

        @Override
        public int size() {
            return pinnedSize();
        }

        @Override
        public boolean contains(final Object o) {
            return SelectableConcurrentHashMap.this.isPinned(o) && SelectableConcurrentHashMap.this.containsKey(o);
        }
    }

    private class PinnedKeyIterator extends KeyIterator {
        @Override
        protected boolean hideValue(final HashEntry hashEntry) {
            return super.hideValue(hashEntry) || !hashEntry.pinned;
        }
    }

    protected static int hash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }
}
