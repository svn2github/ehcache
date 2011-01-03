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
package net.sf.ehcache.store.compound;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.util.VmUtils;

/**
 * Segment implementation used in LocalStore.
 * <p>
 * The segment extends ReentrantReadWriteLock to allow read locking on read operations.
 * In addition to the typical CHM-like methods, this classes additionally supports
 * replacement under a read lock - which is accomplished using an atomic CAS on the
 * associated HashEntry.
 * 
 * @author Chris Dennis
 */
class Segment extends ReentrantReadWriteLock {

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
    private final InternalElementSubstituteFactory primaryFactory;
    
    /**
     * The single identity substitute factory.  Identity substitute factories store the elements as <code>Element</code>s.
     * <p>
     * Only one identity substitute factory can be used in any given Store.  Otherwise there would be ambiguity surrounding counts and
     * factory calls for the stored bare elements.
     */
    private final InternalElementSubstituteFactory identityFactory;
    
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

    private final boolean copyOnRead;
    private final boolean copyOnWrite;

    private final ReadWriteCopyStrategy<Element> copyStrategy;

    /**
     * Create a Segment with the given initial capacity, load factor, and primary element substitute factory.  If the primary factory is not an
     * identity element substitute factory then it will be assumed that there is no identity element substitute factory.
     * 
     * @param initialCapacity initial capacity of store
     * @param loadFactor fraction of capacity at which rehash occurs
     * @param primary primary element substitute factory
     */
    Segment(int initialCapacity, float loadFactor, InternalElementSubstituteFactory primary) {
        this(initialCapacity, loadFactor, primary,
                primary instanceof IdentityElementSubstituteFactory ? (IdentityElementSubstituteFactory) primary : null);
    }

    /**
     * Create a Segment with the given initial capacity, load-factor, primary element substitute factory, and identity element substitute factory.
     * <p>
     * An identity element substitute factory is specified at construction time because only one subclass of IdentityElementProxyFactory
     * can be used with a Segment.  Without this requirement the mapping between bare {@link Element} instances and the factory
     * responsible for them would be ambiguous.
     * <p>
     * If a <code>null</code> identity element substitute factory is specified then encountering a raw element (i.e. as a result of using an
     * identity element substitute factory) will result in a null pointer exception during decode.
     * 
     * @param initialCapacity initial capacity of store
     * @param loadFactor fraction of capacity at which rehash occurs
     * @param primary primary element substitute factory
     * @param identity identity element substitute factory
     */
    Segment(int initialCapacity, float loadFactor, InternalElementSubstituteFactory primary, IdentityElementSubstituteFactory identity) {
        this(initialCapacity, loadFactor, primary, identity, false, false, null);
    }
    
    /**
     * Create a Segment with the given initial capacity, load-factor, primary element substitute factory, and identity element substitute factory.
     * <p>
     * An identity element substitute factory is specified at construction time because only one subclass of IdentityElementProxyFactory
     * can be used with a Segment.  Without this requirement the mapping between bare {@link Element} instances and the factory
     * responsible for them would be ambiguous.
     * <p>
     * If a <code>null</code> identity element substitute factory is specified then encountering a raw element (i.e. as a result of using an
     * identity element substitute factory) will result in a null pointer exception during decode.
     *
     * @param initialCapacity initial capacity of store
     * @param loadFactor fraction of capacity at which rehash occurs
     * @param primary primary element substitute factory
     * @param identity identity element substitute factory
     * @param copyOnRead true should we copy Elements on reads, otherwise false
     * @param copyOnWrite true should we copy Elements on writes, otherwise false
     * @param copyStrategy the strategy to use to copy (can't be null if copyOnRead or copyOnWrite is true)
     */
    Segment(int initialCapacity, float loadFactor, InternalElementSubstituteFactory primary, IdentityElementSubstituteFactory identity, 
            boolean copyOnRead, boolean copyOnWrite, final ReadWriteCopyStrategy<Element> copyStrategy) {
        this.table = new HashEntry[initialCapacity];
        this.threshold = (int) (table.length * loadFactor);
        this.modCount = 0;
        this.primaryFactory = primary;
        this.identityFactory = identity;
        this.copyOnRead = copyOnRead;
        this.copyOnWrite = copyOnWrite;
        if ((copyOnRead || copyOnWrite) && copyStrategy == null) {
            throw new NullPointerException("You need to provide a non-null CopyStrategy if copyOnRead or copyOnWrite is set to true!");
        }
        this.copyStrategy = copyStrategy;
    }
    
    private HashEntry getFirst(int hash) {
        HashEntry[] tab = table;
        return tab[hash & (tab.length - 1)];
    }
    
    /**
     * Decode the possible ElementSubstitute 
     * 
     * @param key
     * @param object
     * @return
     */
    Element decode(Object key, Object object) {
        Element element;
        if (object instanceof Element) {
            element = identityFactory.retrieve(key, object);
        } else {
            InternalElementSubstituteFactory factory = ((ElementSubstitute) object).getFactory();
            element = factory.retrieve(key, object);
        }
        return potentiallyCopyForRead(element, copyOnRead);
    }

    private Element potentiallyCopyForRead(final Element value, final boolean copy) {
        Element newValue;
        if (copy && copyOnRead && copyOnWrite) {
            newValue = copyStrategy.copyForRead(value);
        } else if (copy) {
            newValue = copyStrategy.copyForRead(copyStrategy.copyForWrite(value));
        } else {
            newValue = value;
        }
        return newValue;
    }

    private Element potentiallyCopyForWrite(final Element value, final boolean copy) {
        Element newValue;
        if (copy && copyOnRead && copyOnWrite) {
            newValue = copyStrategy.copyForWrite(value);
        } else if (copy) {
            newValue = copyStrategy.copyForRead(copyStrategy.copyForWrite(value));
        } else {
            newValue = value;
        }
        return newValue;
    }

    private void free(Object object) {
        if (object instanceof Element) {
            identityFactory.free(writeLock(), object);
        } else {
            ((ElementSubstitute) object).getFactory().free(writeLock(), (ElementSubstitute) object);
        }
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
                        return decode(e.key, e.getElement());
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
     * Return true if this segment maps any key the given element.
     * 
     * @param value element to check for
     * @return <code>true</code> if a key is mapped to this element
     */
    boolean containsValue(Element value, ElementValueComparator comparator) {
        readLock().lock();
        try {
            // read-volatile
            if (count != 0) {
                HashEntry[] tab = table;
                int len = tab.length;
                for (int i = 0; i < len; i++) {
                    for (HashEntry e = tab[i]; e != null; e = e.next) {
                        Element element = decode(e.key, e.getElement());
                        if (comparator.equals(value, element)) {
                            return true;
                        }
                    }
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
        Object encoded = create(key, newElement);
        
        writeLock().lock();
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            boolean replaced = false;
            if (e != null && comparator.equals(oldElement, decode(e.key, e.getElement()))) {
                replaced = true;
                /*
                 * make sure we re-get from the HashEntry - since the decode in the conditional
                 * may have faulted in a different type - we must make sure we know what type
                 * to do the increment/decrement on.
                 */
                Object old = e.getElement();
                e.setElement(encoded);
                installed = true;
                free(old);
            } else {
                free(encoded);
            }
            return replaced;
        } finally {
            writeLock().unlock();
            
            if ((installed && encoded instanceof ElementSubstitute)) {
                ((ElementSubstitute) encoded).installed();
            }
        }
    }

    private Object create(final Object key, final Element newElement) {
        return primaryFactory.create(key, potentiallyCopyForWrite(newElement, copyOnWrite));
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
        Object encoded = create(key, newElement);
        
        writeLock().lock();
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            Element oldElement = null;
            if (e != null) {
                Object old = e.getElement();
                e.setElement(encoded);
                installed = true;
                oldElement = decode(null, old);
                free(old);
            } else {
                free(encoded);
            }
            return oldElement;
        } finally {
            writeLock().unlock();
            
            if ((installed && encoded instanceof ElementSubstitute)) {
                ((ElementSubstitute) encoded).installed();
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
        Object encoded = create(key, element);
        
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
                Object old = e.getElement();
                if (!onlyIfAbsent) {
                    e.setElement(encoded);
                    installed = true;
                    oldElement = decode(null, old);
                    free(old);
                } else {
                    free(encoded);
                    oldElement = decode(e.key, old);
                }
            } else {
                oldElement = null;
                ++modCount;
                tab[index] = newHashEntry(key, hash, first, encoded);
                installed = true;
                // write-volatile
                count = count + 1;
            }
            return oldElement;
        } finally {
            writeLock().unlock();
            
            if ((installed && encoded instanceof ElementSubstitute)) {
                ((ElementSubstitute) encoded).installed();
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
     */
    boolean putRawIfAbsent(Object key, int hash, Object encoded) {
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
            if (e == null) {
                oldElement = null;
                ++modCount;
                tab[index] = newHashEntry(key, hash, first, encoded);
                // write-volatile
                count = count + 1;
                return true;
            } else {
                return false;
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
                        newTable[k] = newHashEntry(p.key, p.hash, n, p.getElement());
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
                if (value == null || comparator.equals(value, decode(e.key, e.getElement()))) {
                    // All entries following removed node can stay
                    // in list, but all preceding ones need to be
                    // cloned.
                    ++modCount;
                    HashEntry newFirst = e.next;
                    for (HashEntry p = first; p != e; p = p.next) {
                        newFirst = newHashEntry(p.key, p.hash, newFirst, p.getElement());
                    }
                    tab[index] = newFirst;
                    /*
                     * make sure we re-get from the HashEntry - since the decode in the conditional
                     * may have faulted in a different type - we must make sure we know what type
                     * to do the free on.
                     */
                    Object v = e.getElement();
                    oldValue = decode(null, v);
                    free(v);
                    // write-volatile
                    count = count - 1;
                }
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
        } finally {
            writeLock().unlock();
        }
    }
    
    /**
     * Atomically switch (CAS) the <code>expect</code> representation of this element for the
     * <code>fault</code> representation.
     * <p>
     * A successful switch will return <code>true</code>, and free the replaced element/element-proxy.
     * A failed switch will return <code>false</code> and free the element/element-proxy which was not
     * installed.
     * 
     * @param key key to which this element (proxy) is mapped
     * @param hash spread-hash for this key
     * @param expect element (proxy) expected
     * @param fault element (proxy) to install
     * @return <code>true</code> if <code>fault</code> was installed
     */
    boolean tryFault(Object key, int hash, Object expect, Object fault) {
        boolean installed = false;
        
        if (readLock().tryLock()) {
            try {
                installed = install(key, hash, expect, fault);
                if (installed) {
                    return true;
                }
            } finally {
                readLock().unlock();

                if ((installed && fault instanceof ElementSubstitute)) {
                    ((ElementSubstitute) fault).installed();
                }
            }
        }
        
        free(fault);
        return false;
    }

    boolean fault(Object key, int hash, Object expect, Object fault) {
        boolean installed = false;

        readLock().lock();
        try {
            installed = install(key, hash, expect, fault);
            if (installed) {
                return true;
            }
        } finally {
            readLock().unlock();

            if ((installed && fault instanceof ElementSubstitute)) {
                ((ElementSubstitute) fault).installed();
            }
        }

        free(fault);
        return false;
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
     * Remove the matching mapping.  Unlike the {@link Segment#remove(Object, int, Element, net.sf.ehcache.store.ElementValueComparator)} method
     * evict does referential comparison of the unretrieved substitute against the argument value.
     * 
     * @param key key to match against
     * @param hash spread-hash for the key
     * @param value optional value to match against
     * @return <code>true</code> on a successful remove
     */
    boolean evict(Object key, int hash, Object value) {
        if (writeLock().tryLock()) {
            try {
                HashEntry[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry first = tab[index];
                HashEntry e = first;
                while (e != null && (e.hash != hash || !key.equals(e.key))) {
                    e = e.next;
                }

                if (e != null) {
                    if (value == null || (value == e.getElement())) {
                        // All entries following removed node can stay
                        // in list, but all preceding ones need to be
                        // cloned.
                        ++modCount;
                        HashEntry newFirst = e.next;
                        for (HashEntry p = first; p != e; p = p.next) {
                            newFirst = newHashEntry(p.key, p.hash, newFirst, p.getElement());
                        }
                        tab[index] = newFirst;
                        /*
                         * make sure we re-get from the HashEntry - since the decode in the conditional
                         * may have faulted in a different type - we must make sure we know what type
                         * to do the free on.
                         */
                        Object v = e.getElement();
                        free(v);
                        // write-volatile
                        count = count - 1;
                        return true;
                    }
                }
                
                return false;
            } finally {
                writeLock().unlock();
            }
        } else {
            return false;
        }
    }

    /**
     * Select a random sample of elements generated by the supplied factory.
     * 
     * @param <T> type of the elements or element substitutes
     * @param filter filter of substitute types
     * @param sampleSize minimum number of elements to return
     * @param sampled collection in which to place the elements
     * @param seed random seed for the selection
     */
    <T> void addRandomSample(ElementSubstituteFilter<T> filter, int sampleSize, Collection<T> sampled, int seed) {
        final HashEntry[] tab = table;
        final int tableStart = seed & (tab.length - 1);
        int tableIndex = tableStart;
        do {
            for (HashEntry e = tab[tableIndex]; e != null; e = e.next) {
                Object value = e.getElement();
                if (filter.allows(value)) {
                    sampled.add((T) value);
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
     */
    Iterator<HashEntry> hashIterator() {
        return new HashIterator();
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
    
    private HashEntry newHashEntry(Object key, int hash, HashEntry newFirst, Object element) {
        if (VmUtils.isInGoogleAppEngine()) {
            return new SynchronizedHashEntry(key, hash, newFirst, element);
        }
        return new AtomicHashEntry(key, hash, newFirst, element);
    }

}
