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
package net.sf.ehcache.store.chm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.Element;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.pool.PoolAccessor;

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

    private final Random rndm = new Random();
    private final PoolAccessor poolAccessor;
    private final boolean elementPinningEnabled;
    private volatile long maxSize;
    private final RegisteredEventListeners cacheEventNotificationService;

    public SelectableConcurrentHashMap(PoolAccessor poolAccessor, boolean elementPinningEnabled, int initialCapacity, float loadFactor, int concurrency, final long maximumSize, final RegisteredEventListeners cacheEventNotificationService) {
        super(initialCapacity, loadFactor, concurrency);
        this.poolAccessor = poolAccessor;
        this.elementPinningEnabled = elementPinningEnabled;
        this.maxSize = maximumSize;
        this.cacheEventNotificationService = cacheEventNotificationService;
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
                    if (value != null && (value.isExpired() || !(value.isPinned() && elementPinningEnabled))) {
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
        return new MemoryStoreHashEntry(null, 0, null, e, 0);
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
        long size = 0;

        for (Segment<Object, Element> segment : this.segments) {
            size += segment.count;
        }

        if (size > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        return (int) size;
    }


    public ReentrantReadWriteLock lockFor(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash);
    }

    public ReentrantReadWriteLock[] locks() {
        return segments;
    }

    public Element put(Object key, Element element, long sizeOf) {
        int hash = hash(key.hashCode());
        return ((MemoryStoreSegment) segmentFor(hash)).put(key, hash, element, sizeOf, false);
    }

    public Element putIfAbsent(Object key, Element element, long sizeOf) {
        int hash = hash(key.hashCode());
        return ((MemoryStoreSegment) segmentFor(hash)).put(key, hash, element, sizeOf, true);
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

    final class MemoryStoreSegment extends Segment<Object, Element> {

        private Iterator<MemoryStoreHashEntry> evictionIterator = iterator();
        private boolean fullyPinned;

        private MemoryStoreSegment(int initialCapacity, float lf) {
            super(initialCapacity, lf);
        }

        @Override
        protected HashEntry<Object, Element> relinkHashEntry(HashEntry<Object, Element> e, HashEntry<Object, Element> next) {
            if (e instanceof MemoryStoreHashEntry) {
                MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                return new MemoryStoreHashEntry(mshe.key, mshe.hash, next, mshe.value, mshe.sizeOf);
            } else {
                return new HashEntry<Object, Element>(e.key, e.hash, next, e.value);
            }
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
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        // All entries following removed node can stay
                        // in list, but all preceding ones need to be
                        // cloned.
                        ++modCount;
                        HashEntry<Object,Element> newFirst = e.next;
                        for (HashEntry<Object,Element> p = first; p != e; p = p.next)
                            newFirst = relinkHashEntry(p, newFirst);
                        tab[index] = newFirst;
                        count = c; // write-volatile
                        MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                        poolAccessor.delete(mshe.sizeOf);
                    }
                }
                return oldValue;
            } finally {
                writeLock().unlock();
            }
        }


        Element put(Object key, int hash, Element value, long sizeOf, boolean onlyIfAbsent) {
            Element evicted = null;
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
                    if (!onlyIfAbsent) {
                        MemoryStoreHashEntry mshe = (MemoryStoreHashEntry) e;
                        poolAccessor.delete(mshe.sizeOf);
                        e.value = value;
                        mshe.sizeOf = sizeOf;
                    }
                }
                else {
                    oldValue = null;
                    ++modCount;
                    tab[index] = new MemoryStoreHashEntry(key, hash, first, value, sizeOf);
                    count = c; // write-volatile
                }
                if(onlyIfAbsent && oldValue != null || !onlyIfAbsent) {
                    if (!value.isPinned()) {
                        this.fullyPinned = false;
                    }
                    if (SelectableConcurrentHashMap.this.maxSize > 0
                        && SelectableConcurrentHashMap.this.quickSize() > SelectableConcurrentHashMap.this.maxSize) {
                        Element evict = nextExpiredOrToEvict();
                        if (evict != null) {
                            evicted = remove(evict.getKey(), hash(evict.getKey().hashCode()), null);
                        }
                    }
                }
                return oldValue;
            } finally {
                writeLock().unlock();
                notifyEvictionOrExpiry(evicted);
            }
        }

        private void notifyEvictionOrExpiry(final Element element) {
            if(element != null) {
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
                        if (e.hash == hash && key.equals(e.key)) {
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

        private Element nextExpiredOrToEvict() {

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
                    final boolean pinned = next.value.isPinned();
                    if (!pinned) {
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
                Element evict = nextExpiredOrToEvict();
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

        volatile long sizeOf;
        volatile boolean accessed = true;

        private MemoryStoreHashEntry(Object key, int hash, HashEntry<Object, Element> next, Element value, long sizeOf) {
            super(key, hash, next, value);
            this.sizeOf = sizeOf;
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
            throw new UnsupportedOperationException("DON'T YOU DO THIS!");
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
}
