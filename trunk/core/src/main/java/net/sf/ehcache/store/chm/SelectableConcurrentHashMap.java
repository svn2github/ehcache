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

import java.io.Serializable;
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
    public static final Element DUMMY_PINNED_ELEMENT = new Element(new DummyPinnedKey(), new DummyPinnedValue());
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
        return quickSizeInternal(false);
    }

    private int quickSizeInternal(final boolean grabAllSegmentsLock) {
        long size = 0;
        long numDummyPinnedKeys = 0;
        if(grabAllSegmentsLock) {
            grabAllSegmentsLock();
        }
        try {
            for (Segment<Object, Element> segment : this.segments) {
                MemoryStoreSegment mss = (MemoryStoreSegment)segment;
                size += segment.count;
                numDummyPinnedKeys += mss.numDummyPinnedKeys;
            }

            long quickSize = size - numDummyPinnedKeys;

            if (quickSize > Integer.MAX_VALUE)
                return Integer.MAX_VALUE;
            return (int)quickSize;
        } finally {
            if(grabAllSegmentsLock) {
                releaseAllSegmentsLock();
            }
        }
    }

    @Override
    public int size() {
        return quickSizeInternal(true);
    }

    private void grabAllSegmentsLock() {
        for (int i = 0; i < segments.length; ++i)
            segments[i].readLock().lock();
    }

    private void releaseAllSegmentsLock() {
        for (int i = 0; i < segments.length; ++i)
            segments[i].readLock().unlock();
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
        return new KeySet();
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
        private volatile int numDummyPinnedKeys;

        private MemoryStoreSegment(int initialCapacity, float lf) {
            super(initialCapacity, lf);
        }

        private void calculateEmptyPinnedKeySize(boolean pinned, MemoryStoreHashEntry mshe) {
            writeLock().lock();
            try {
                if(mshe == null && pinned) {//want to pin first time
                    ++numDummyPinnedKeys;
                    return;
                }
                if(mshe == null || (pinned == mshe.pinned) || (!mshe.pinned && !pinned)) {
                  // 1. want to unpin which is not present
                  // 2. want to pin/unpin again (same operation)
                  // 3. want to unpin which was never pinned
                    return;
                }
                if(pinned) {//want to pin
                    ++numDummyPinnedKeys;
                } else {//want to unpin
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
            if(numDummyPinnedKeys == count) {
                clear();
                return;
            }
            try {
                Iterator<MemoryStoreHashEntry> itr = iterator();
                // using clock iterator here so maintaining number of visited entries
                int numVisited = 0;
                while(itr.hasNext() && numVisited < count) {
                    MemoryStoreHashEntry mshe = itr.next();
                    mshe.setPinned(false);
                    ++numVisited;
                }
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
                            ++numDummyPinnedKeys;
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

        Element putInternal(Object key, int hash, Element value, long sizeOf, boolean onlyIfAbsent, boolean pinned) {
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
                        long runs = Math.min(5, SelectableConcurrentHashMap.this.quickSize() - SelectableConcurrentHashMap.this.maxSize);
                        while (runs-- > 0) {
                            Element evict = nextExpiredOrToEvict(value);
                            if (evict != null) {
                                evicted = remove(evict.getKey(), hash(evict.getKey().hashCode()), null);
                            }
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
            return new HashEntryIterator();
        }

        @Override
        public int size() {
            return SelectableConcurrentHashMap.this.size();
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

    final class HashEntryIterator extends HashIterator implements Iterator<Object> {
        private HashEntry<Object, Element> myNextEntry;

        public HashEntryIterator() {
            advanceToNextNonSentinelEntry();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }

        public Object next() {
            if (myNextEntry == null) {
                throw new NoSuchElementException();
            }
            Object key = myNextEntry.value.getObjectKey();
            advanceToNextNonSentinelEntry();
            return key;
        }

        @Override
        public boolean hasNext() {
            return myNextEntry != null;
        }

        private void advanceToNextNonSentinelEntry() {
            HashEntry<Object, Element> myEntry = null;
            while (super.hasNext()) {
                myEntry = nextEntry();
                if (myEntry != null && !isSentinelEntry(myEntry)) {
                    break;
                } else {
                    myEntry = null;
                }
            }
            myNextEntry = myEntry;
        }

        private boolean isSentinelEntry(HashEntry<Object, Element> hashEntry) {
            MemoryStoreHashEntry mshe = (MemoryStoreHashEntry)hashEntry;
            return mshe.value == DUMMY_PINNED_ELEMENT;
        }
    }

}
