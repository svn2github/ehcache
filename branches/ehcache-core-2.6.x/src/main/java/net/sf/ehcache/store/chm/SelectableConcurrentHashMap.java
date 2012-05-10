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
import java.util.Collection;
import java.util.Iterator;
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
public class SelectableConcurrentHashMap extends ConcurrentHashMap<Object, Element> {
    private static final Element DUMMY_PINNED_ELEMENT = new Element(new DummyPinnedKey(), new DummyPinnedValue());
    private final Random rndm = new Random();
    private final PoolAccessor poolAccessor;
    private final boolean elementPinningEnabled;
    private volatile long maxSize;
    private volatile SelectableConcurrentHashMap.PinnedKeySet pinnedKeySet;
    private final RegisteredEventListeners cacheEventNotificationService;

    public SelectableConcurrentHashMap(PoolAccessor poolAccessor, boolean elementPinningEnabled, int initialCapacity, float loadFactor, int concurrency, final long maximumSize, final RegisteredEventListeners cacheEventNotificationService) {
        super(initialCapacity, loadFactor, concurrency);
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
            final HashEntry<Object, Element>[] table = segments[segmentIndex].table;
            final int tableStart = randomHash & (table.length - 1);
            int tableIndex = tableStart;
            do {
                for (HashEntry<Object, Element> e = table[tableIndex]; e != null; e = e.next) {
                    Element value = e.value;
                    MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                    if (value != null && (value.isExpired() || !(mshe.pinned && elementPinningEnabled))) {
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
        return new MemoryStoreHashEntry(null, 0, null, e, 0, false);
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
        final Segment<?, ?>[] segments = this.segments;
        long sum = 0;
        for (Segment<?, ?> seg : segments) {
            sum += seg.count - ((MemoryStoreSegment) seg).numDummyPinnedKeys;
        }

        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int)sum;
        }
    }

    @Override
    public int size() {
        final Segment<?, ?>[] segments = this.segments;

        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            int[] mc = new int[segments.length];
            long check = 0;
            long sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].count - ((MemoryStoreSegment) segments[i]).numDummyPinnedKeys;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    check += segments[i].count - ((MemoryStoreSegment) segments[i]).numDummyPinnedKeys;
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
                sum += segments[i].count - ((MemoryStoreSegment) segments[i]).numDummyPinnedKeys;
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
        final Segment<?, ?>[] segments = this.segments;

        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            int[] mc = new int[segments.length];
            long check = 0;
            long sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                sum += ((MemoryStoreSegment) segments[i]).pinnedCount - ((MemoryStoreSegment) segments[i]).numDummyPinnedKeys;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    check += ((MemoryStoreSegment) segments[i]).pinnedCount - ((MemoryStoreSegment) segments[i]).numDummyPinnedKeys;
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
                sum += ((MemoryStoreSegment) segments[i]).pinnedCount - ((MemoryStoreSegment) segments[i]).numDummyPinnedKeys;
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

    @Override
    public Element put(final Object key, final Element value) {
        return put(key, value, 0);
    }

    @Override
    public Element putIfAbsent(final Object key, final Element value) {
        return putIfAbsent(key, value, 0);
    }

    public Element put(Object key, Element element, long sizeOf) {
        int hash = hash(key.hashCode());
        return ((MemoryStoreSegment) segmentFor(hash)).put(key, hash, element, sizeOf, false);
    }

    public Element putIfAbsent(Object key, Element element, long sizeOf) {
        int hash = hash(key.hashCode());
        return ((MemoryStoreSegment) segmentFor(hash)).put(key, hash, element, sizeOf, true);
    }

    public void unpinAll() {
        for (Segment<Object, Element> segment : this.segments) {
            MemoryStoreSegment mss = (MemoryStoreSegment)segment;
            mss.unpinAll();
        }
    }

    public void setPinned(Object key, boolean pinned) {
        int hash = hash(key.hashCode());
        ((MemoryStoreSegment) segmentFor(hash)).setPinned(key, pinned, hash);
    }

    public boolean isPinned(Object key) {
        int hash = hash(key.hashCode());
        return ((MemoryStoreSegment) segmentFor(hash)).isPinned(key, hash);
    }

    @Override
    public Set<Object> keySet() {
        Set<Object> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet());
    }

    @Override
    public Collection<Element> values() {
        Collection<Element> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    @Override
    public Set<Entry<Object, Element>> entrySet() {
        Set<Entry<Object, Element>> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    @Override
    protected Segment<Object, Element> createSegment(int initialCapacity, float lf) {
        return new MemoryStoreSegment(initialCapacity, lf);
    }

    public boolean evict() {
        return getRandomSegment().evict();
    }

    private MemoryStoreSegment getRandomSegment() {
        int randomHash = rndm.nextInt();
        return (MemoryStoreSegment) segments[((randomHash >>> segmentShift) & segmentMask)];
    }

    public void recalculateSize(Object key) {
        int hash = hash(key.hashCode());
        ((MemoryStoreSegment) segmentFor(hash)).recalculateSize(key, hash);
    }

    public Set pinnedKeySet() {
        Set<Object> pks = pinnedKeySet;
        return (pks != null) ? pks : (pinnedKeySet = new PinnedKeySet());
    }

    final class MemoryStoreSegment extends Segment<Object, Element> {

        private static final int MAX_EVICTION = 5;
        private Iterator<MemoryStoreHashEntry> evictionIterator = iterator();
        private boolean fullyPinned;
        private volatile int pinnedCount;
        private volatile int numDummyPinnedKeys;

        private MemoryStoreSegment(int initialCapacity, float lf) {
            super(initialCapacity, lf);
        }

        private void calculateEmptyPinnedKeySize(boolean pinned, MemoryStoreHashEntry mshe) {
            writeLock().lock();
            try {
                if(mshe == null && pinned) {//want to pin first time
                    ++pinnedCount;
                    ++numDummyPinnedKeys;
                    return;
                }
                if(pinned) {
                    ++pinnedCount;
                    return;
                }
                if(mshe == null || (!mshe.pinned && !pinned)) {
                  // 1. want to unpin which is not present
                  // 2. want to pin/unpin again (same operation)
                  // 3. want to unpin which was never pinned
                    return;
                }
                if(pinned) {//want to pin
                    ++pinnedCount;
                    ++numDummyPinnedKeys;
                } else {//want to unpin
                    --pinnedCount;
                    --numDummyPinnedKeys;
                }
            } finally {
                writeLock().unlock();
            }
        }

        public void setPinned(Object key, boolean pinned, int hash) {
            writeLock().lock();
            try {
                HashEntry<Object,Element>[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<Object,Element> first = tab[index];
                HashEntry<Object,Element> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;
                MemoryStoreHashEntry mshe = null;
                if (e != null) {
                    mshe = (MemoryStoreHashEntry) e;
                    mshe.setPinned(pinned);
                } else {
                    if(pinned) {
                        putInternal(key, hash, DUMMY_PINNED_ELEMENT, 0, false, true);
                    }
                }
                calculateEmptyPinnedKeySize(pinned, mshe);
            } finally {
                writeLock().unlock();
            }
        }

        public boolean isPinned(Object key, int hash) {
            readLock().lock();
            try {
                HashEntry<Object,Element>[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<Object,Element> first = tab[index];
                HashEntry<Object,Element> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;
                if (e != null) {
                    return ((MemoryStoreHashEntry) e).pinned;
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
                Iterator<MemoryStoreHashEntry> itr = iterator();
                // using clock iterator here so maintaining number of visited entries
                int numVisited = 0;
                int dummyPinnedKeys = 0;
                while(itr.hasNext() && numVisited < count) {
                    MemoryStoreHashEntry mshe = itr.next();
                    if(mshe.pinned && mshe.value == DUMMY_PINNED_ELEMENT) {
                        ++dummyPinnedKeys;
                    }
                    mshe.setPinned(false);
                    ++numVisited;
                }
                pinnedCount = numDummyPinnedKeys = dummyPinnedKeys;
            } finally {
                writeLock().unlock();
            }
        }

        @Override
        protected HashEntry<Object, Element> relinkHashEntry(HashEntry<Object, Element> e, HashEntry<Object, Element> next) {
            if (e instanceof MemoryStoreHashEntry) {
                MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                return new MemoryStoreHashEntry(mshe.key, mshe.hash, next, mshe.value, mshe.sizeOf, mshe.pinned);
            } else {
                return new HashEntry<Object, Element>(e.key, e.hash, next, e.value);
            }
        }

        @Override
        void clear() {
            super.clear();
            numDummyPinnedKeys = 0;
            pinnedCount = 0;
        }

        @Override
        Element remove(Object key, int hash, Object value) {
            writeLock().lock();
            try {
                int c = count - 1;
                HashEntry<Object,Element>[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<Object,Element> first = tab[index];
                HashEntry<Object,Element> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                Element oldValue = null;
                if (e != null) {
                    Element v = e.value;
                    MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        ++modCount;
                        if(!mshe.pinned) {
                            // All entries following removed node can stay
                            // in list, but all preceding ones need to be
                            // cloned.
                            HashEntry<Object,Element> newFirst = e.next;
                            for (HashEntry<Object,Element> p = first; p != e; p = p.next)
                                newFirst = relinkHashEntry(p, newFirst);
                            tab[index] = newFirst;
                        } else {
                            ++c;
                            mshe.value = DUMMY_PINNED_ELEMENT;
                            if (oldValue == DUMMY_PINNED_ELEMENT) {
                               oldValue = null;
                            } else {
                                ++numDummyPinnedKeys;
                            }
                        }
                        count = c; // write-volatile
                        poolAccessor.delete(mshe.sizeOf);
                    }
                }
                return oldValue;
            } finally {
                writeLock().unlock();
            }
        }

        Element put(Object key, int hash, Element value, long sizeOf, boolean onlyIfAbsent) {
            return putInternal(key, hash, value, sizeOf, onlyIfAbsent, false);
        }

        public void recalculateSize(Object key, int hash) {
            Element value = null;
            long oldSize = 0;
            readLock().lock();
            try {
                HashEntry<Object, Element>[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<Object, Element> first = tab[index];
                HashEntry<Object, Element> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key))) {
                    e = e.next;
                }
                if (e != null) {
                    MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                    key = mshe.key;
                    value = mshe.value;
                    oldSize = mshe.sizeOf;
                }
            } finally {
                readLock().unlock();
            }
            if (value != null) {
                long delta = poolAccessor.replace(oldSize, key, value, storedObject(value), true);
                writeLock().lock();
                try {
                    HashEntry<Object, Element>[] tab = table;
                    int index = hash & (tab.length - 1);
                    HashEntry<Object, Element> first = tab[index];
                    HashEntry<Object, Element> e = first;
                    while (e != null && key != e.key) {
                        e = e.next;
                    }

                    if (e != null && e.value == value && oldSize == ((MemoryStoreHashEntry) e).sizeOf) {
                        ((MemoryStoreHashEntry) e).sizeOf = oldSize + delta;
                    } else {
                        poolAccessor.delete(delta);
                    }
                } finally {
                    writeLock().unlock();
                }
            }
        }

        Element putInternal(Object key, int hash, Element value, long sizeOf, boolean onlyIfAbsent, boolean pinned) {
            Element[] evicted = new Element[MAX_EVICTION];
            writeLock().lock();
            try {
                int c = count;
                if (c++ > threshold) // ensure capacity
                    rehash();
                HashEntry<Object,Element>[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<Object,Element> first = tab[index];
                HashEntry<Object,Element> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key)))
                    e = e.next;

                Element oldValue;
                if (e != null) {
                    oldValue = e.value;
                    if (e.value == DUMMY_PINNED_ELEMENT || !onlyIfAbsent) {
                        MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                        poolAccessor.delete(mshe.sizeOf);
                        e.value = value;
                        mshe.sizeOf = sizeOf;
                        if (oldValue == DUMMY_PINNED_ELEMENT && value != DUMMY_PINNED_ELEMENT) {
                            --numDummyPinnedKeys;
                            oldValue = null;
                        }
                    }
                }
                else {
                    oldValue = null;
                    ++modCount;
                    tab[index] = new MemoryStoreHashEntry(key, hash, first, value, sizeOf, pinned);
                    count = c; // write-volatile
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

        @Override
        Element get(final Object key, final int hash) {
            readLock().lock();
            try {
                if (count != 0) { // read-volatile
                    HashEntry<Object,Element> e = getFirst(hash);
                    while (e != null) {
                        if (e.hash == hash && key.equals(e.key) && !e.value.equals(DUMMY_PINNED_ELEMENT)) {
                            ((MemoryStoreHashEntry)e).accessed = true;
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

        @Override
        boolean containsKey(final Object key, final int hash) {
            readLock().lock();
            try {
                if (count != 0) { // read-volatile
                    HashEntry<Object,Element> e = getFirst(hash);
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

        private Element nextExpiredOrToEvict(final Element justAdded) {

            Element lastUnpinned = null;
            int i = 0;

            while (!fullyPinned && i++ < count) {
                if (!evictionIterator.hasNext()) {
                    evictionIterator = iterator();
                }
                final MemoryStoreHashEntry next = evictionIterator.next();
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

        private Iterator<MemoryStoreHashEntry> iterator() {
            return new MemoryStoreHashEntryIterator(this);
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
    }

    static final class MemoryStoreHashEntry extends HashEntry<Object, Element> {
        volatile boolean pinned;
        volatile long sizeOf;
        volatile boolean accessed = true;

        private MemoryStoreHashEntry(Object key, int hash, HashEntry<Object, Element> next, Element value, long sizeOf, boolean pinned) {
            super(key, hash, next, value);
            this.sizeOf = sizeOf;
            this.pinned = pinned;
        }

        public void setPinned(boolean pinned) {
            this.pinned = pinned;
        }
    }

    private class MemoryStoreHashEntryIterator implements Iterator<MemoryStoreHashEntry> {

        int nextTableIndex;
        HashEntry[] currentTable;
        MemoryStoreHashEntry nextEntry;
        MemoryStoreHashEntry lastReturned;
        private final MemoryStoreSegment seg;

        private MemoryStoreHashEntryIterator(final MemoryStoreSegment memoryStoreSegment) {
            nextTableIndex = -1;
            this.seg = memoryStoreSegment;
            advance();
        }

        public boolean hasNext() {
            return nextEntry != null;
        }

        public MemoryStoreHashEntry next() {
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
            if (nextEntry != null && (nextEntry = (MemoryStoreHashEntry)nextEntry.next) != null)
                return;
            while (nextTableIndex >= 0) {
                if ( (nextEntry = (MemoryStoreHashEntry) currentTable[nextTableIndex--]) != null)
                    return;
            }
            if (seg.count != 0) {
                currentTable = seg.table;
                for (int j = currentTable.length - 1; j >= 0; --j) {
                    if ( (nextEntry = (MemoryStoreHashEntry)currentTable[j]) != null) {
                        nextTableIndex = j - 1;
                        return;
                    }
                }
            }
        }
    }

    @IgnoreSizeOf
    private static class DummyPinnedKey implements Serializable {

    }

    @IgnoreSizeOf
    private static class DummyPinnedValue implements Serializable {

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

        public Object next() {
            return nextEntry().key;
        }
    }

    final class ValueIterator extends HashEntryIterator implements Iterator<Element> {

        public Element next() {
            return nextEntry().value;
        }
    }

    final class EntryIterator extends HashEntryIterator implements Iterator<Entry<Object, Element>> {

        public Entry<Object, Element> next() {
            HashEntry<Object, Element> entry = nextEntry();
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
        private HashEntry<Object, Element> myNextEntry;

        public HashEntryIterator() {
            myNextEntry = advanceToNextEntry();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }

        @Override
        public HashEntry<Object, Element> nextEntry() {
            if (myNextEntry == null) {
                throw new NoSuchElementException();
            }
            HashEntry<Object, Element> entry = myNextEntry;
            myNextEntry = advanceToNextEntry();
            return entry;
        }

        @Override
        public boolean hasNext() {
            return myNextEntry != null;
        }

        private HashEntry<Object, Element> advanceToNextEntry() {
            HashEntry<Object, Element> myEntry = null;
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

        protected boolean hideValue(HashEntry<Object, Element> hashEntry) {
            MemoryStoreHashEntry mshe = (MemoryStoreHashEntry)hashEntry;
            return mshe.value == DUMMY_PINNED_ELEMENT;
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
        protected boolean hideValue(final HashEntry<Object, Element> hashEntry) {
            return super.hideValue(hashEntry) || !((MemoryStoreHashEntry)hashEntry).pinned;
        }
    }
}
