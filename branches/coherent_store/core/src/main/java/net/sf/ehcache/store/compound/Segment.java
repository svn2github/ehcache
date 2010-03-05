/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.Element;

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
     * The primary proxy factory.
     * <p>
     * This is the proxy type used to store <code>Element</code>s when they are first added to the store.
     */
    private final InternalElementProxyFactory primaryFactory;
    
    /**
     * The single identity proxy factory.  Identity proxy factories store the elements as <code>Element</code>s.
     * <p>
     * Only one identity proxy factory can be used in any given Store.  Otherwise there would be ambiguity surrounding counts and
     * factory calls for the stored bare elements.
     */
    private final InternalElementProxyFactory identityFactory;
    
    /**
     * Map from proxy factory instance to AtomicInteger representing the count of elements of that type.
     * <p>
     * We use an unlocked HashMap in a multi-threaded context here.  The map is never mutated and visibility is guaranteed by the final
     * field freeze - fun times!
     */
    private final Map<InternalElementProxyFactory, AtomicInteger> counts;
    
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

    /**
     * Create a Segment with the given initial capacity, load-factor, primary element proxy factory, and set of potential proxy factories.
     * <p>
     * The set of factories used by this segment is specified at construction time for two reasons:
     * <ol>
     *     <li>Only one subclass of IdentityElementProxyFactory can be used with a Segment.  Without this requirement the mapping between
     *     bare {@link Element} instances and the factory responsible for them would be ambiguous.</li>
     *     <li>To avoid having to use a ConcurrentHashMap and a whole load of putIfAbsent logic to on demand create mappings in the
     *     <code>counts</code> map, we instead load the whole thing on construction and rely on final field freeze semantics to ensure
     *     visibility to all threads.</li>
     * </ol>
     * @param initialCapacity initial capacity of store
     * @param loadFactor fraction of capacity at which rehash occurs
     * @param primary primary element proxy factory
     * @param factories set of proxy factories to be used
     */
    Segment(int initialCapacity, float loadFactor, InternalElementProxyFactory primary, Set<InternalElementProxyFactory> factories) {
        this.table = new HashEntry[initialCapacity];
        this.threshold = (int) (table.length * loadFactor);
        this.modCount = 0;
        this.primaryFactory = primary;
        this.identityFactory = validate(primary, factories);
        this.counts = new HashMap<InternalElementProxyFactory, AtomicInteger>(factories.size());
        for (InternalElementProxyFactory f : factories) {
            counts.put(f, new AtomicInteger());
        }
    }
    
    private static IdentityElementProxyFactory validate(InternalElementProxyFactory primary, Set<InternalElementProxyFactory> factories) {
        if (!factories.contains(primary)) {
            throw new IllegalArgumentException("The set of factories " + factories + " does not contain the primary factory " + primary);
        }
        
        IdentityElementProxyFactory identityFactory = null;
        for (InternalElementProxyFactory f : factories) {
            if (f instanceof IdentityElementProxyFactory) {
                if (identityFactory == null) {
                    identityFactory = (IdentityElementProxyFactory) f;
                } else {
                    throw new IllegalArgumentException("The set of factories " + factories
                            + " contains more than one IdentityElementProxyFactory");
                }
            }
        }
        
        return identityFactory;
    }
    
    private HashEntry getFirst(int hash) {
        HashEntry[] tab = table;
        return tab[hash & (tab.length - 1)];
    }
    
    private Element decode(Object key, Object object) {
        if (object instanceof Element) {
            return identityFactory.decode(key, object);
        } else {
            InternalElementProxyFactory factory = ((ElementProxy) object).getFactory();
            return factory.decode(null, object);
        }
    }
    
    private void free(Object object) {
        if (object instanceof Element) {
            identityFactory.free(object);
        } else {
            ((ElementProxy) object).getFactory().free((ElementProxy) object);
        }
    }
    
    private void incrementCount(Object element) {
        if (element instanceof Element) {
            incrementCount(identityFactory);
        } else {
            incrementCount(((ElementProxy) element).getFactory());
        }
    }

    private void incrementCount(InternalElementProxyFactory factory) {
        counts.get(identityFactory).incrementAndGet();
    }

    private void decrementCount(Object element) {
        if (element instanceof Element) {
            decrementCount(identityFactory);
        } else {
            decrementCount(((ElementProxy) element).getFactory());
        }
    }

    private void decrementCount(InternalElementProxyFactory factory) {
        counts.get(identityFactory).decrementAndGet();
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
    boolean containsValue(Object value) {
        readLock().lock();
        try {
            // read-volatile
            if (count != 0) {
                HashEntry[] tab = table;
                int len = tab.length;
                for (int i = 0; i < len; i++) {
                    for (HashEntry e = tab[i]; e != null; e = e.next) {
                        Element element = decode(e.key, e.getElement());
                        if (value.equals(element)) {
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
     * @param key key to map the element to
     * @param hash spread-hash for the key
     * @param oldElement expected element
     * @param newElement element to add
     * @return <code>true</code> on a successful replace
     */
    boolean replace(Object key, int hash, Element oldElement, Element newElement) {
        writeLock().lock();
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            boolean replaced = false;
            if (e != null && oldElement.equals(decode(e.key, e.getElement()))) {
                replaced = true;
                /*
                 * make sure we re-get from the HashEntry - since the decode in the conditional
                 * may have faulted in a different type - we must make sure we know what type
                 * to do the increment/decrement on.
                 */
                Object old = e.getElement();
                e.setElement(primaryFactory.encode(e.key, newElement));
                incrementCount(primaryFactory);
                decrementCount(old);
                free(old);
            }
            return replaced;
        } finally {
            writeLock().unlock();
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
        writeLock().lock();
        try {
            HashEntry e = getFirst(hash);
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            Element oldElement = null;
            if (e != null) {
                Object old = e.getElement();
                e.setElement(primaryFactory.encode(e.key, newElement));
                incrementCount(primaryFactory);
                decrementCount(old);
                oldElement = decode(null, old);
                free(old);
            }
            return oldElement;
        } finally {
            writeLock().unlock();
        }
    }

    /**
     * Add the supplied mapping.
     * <p>
     * The supplied element is proxied using the primary element proxy factory
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
        writeLock().lock();
        try {
            int c = count;
            // ensure capacity
            if (c++ > threshold) {
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
                    e.setElement(primaryFactory.encode(e.key, element));
                    incrementCount(primaryFactory);
                    decrementCount(old);
                    oldElement = decode(null, old);
                    free(old);
                } else {
                    oldElement = decode(e.key, old);
                }
            } else {
                oldElement = null;
                ++modCount;
                tab[index] = new HashEntry(key, hash, first, primaryFactory.encode(key, element));
                incrementCount(primaryFactory);
                // write-volatile
                count = c;
            }
            return oldElement;
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
                        newTable[k] = new HashEntry(p.key, p.hash, n, p.getElement());
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
     * @param key key to match against
     * @param hash spread-hash for the key
     * @param value optional value to match against
     * @return removed element
     */
    Element remove(Object key, int hash, Object value) {
        writeLock().lock();
        try {
            int c = count - 1;
            HashEntry[] tab = table;
            int index = hash & (tab.length - 1);
            HashEntry first = tab[index];
            HashEntry e = first;
            while (e != null && (e.hash != hash || !key.equals(e.key))) {
                e = e.next;
            }

            Element oldValue = null;
            if (e != null) {
                if (value == null || value.equals(decode(e.key, e.getElement()))) {
                    // All entries following removed node can stay
                    // in list, but all preceding ones need to be
                    // cloned.
                    ++modCount;
                    HashEntry newFirst = e.next;
                    for (HashEntry p = first; p != e; p = p.next) {
                        newFirst = new HashEntry(p.key, p.hash, newFirst, p.getElement());
                    }
                    tab[index] = newFirst;
                    /*
                     * make sure we re-get from the HashEntry - since the decode in the conditional
                     * may have faulted in a different type - we must make sure we know what type
                     * to do the increment/decrement on.
                     */
                    Object v = e.getElement();
                    decrementCount(v);
                    oldValue = decode(null, v);
                    free(v);
                    // write-volatile
                    count = c;
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
                    tab[i] = null;
                }
                ++modCount;
                // write-volatile
                count = 0;
                
                for (Entry<InternalElementProxyFactory, AtomicInteger> entry : counts.entrySet()) {
                    entry.getKey().freeAll();
                    entry.getValue().set(0);
                }
            }
        } finally {
            writeLock().unlock();
        }
    }
    
    /**
     * Return the count of elements/element proxies generated by the given factory present in this segment.
     * <p>
     * Passing a factory instance that is not used by this segment will simply result in a zero return.
     * 
     * @param factory an {@link ElementProxyFactory} or {@link IdentityElementProxyFactory} instance used by this segment
     * @return count of elements generated by this factory in this segment
     */
    public int size(InternalElementProxyFactory factory) {
        AtomicInteger i = counts.get(factory);
        if (i == null) {
            return 0;
        } else {
            return i.get();
        }
    }
    
    /**
     * Atomically switch (CAS) the <code>expect</code> representation of this element for the
     * <code>fault</code> representation.
     * <p>
     * A successful switch will return <code>true</code>, modify the associated counts and free
     * the replaced element/element-proxy.  A failed switch will return <code>false</code> and
     * free the element/element-proxy which was not installed.
     * 
     * @param key key to which this element (proxy) is mapped
     * @param hash spread-hash for this key
     * @param expect element (proxy) expected
     * @param fault element (proxy) to install
     * @return <code>true</code> if the <code>fault</code> was installed
     */
    public boolean fault(Object key, int hash, Object expect, Object fault) {
        readLock().lock();
        try {
            if (count != 0) {
                for (HashEntry e = getFirst(hash); e != null; e = e.next) {
                    if (e.hash == hash && key.equals(e.key)) {
                        if (e.casElement(expect, fault)) {
                            incrementCount(fault);
                            decrementCount(expect);
                            free(expect);
                            return true;
                        } else {
                            free(fault);
                            return false;
                        }
                    }
                }
            }
            return false;
        } finally {
            readLock().unlock();
        }
    }
}