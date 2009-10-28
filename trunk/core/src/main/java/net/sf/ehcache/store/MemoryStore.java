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

package net.sf.ehcache.store;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

/**
 * A Store implementation suitable for fast, concurrent in memory stores. The policy is determined by that
 * configured in the cache.
 *
 * @author <a href="mailto:ssuravarapu@users.sourceforge.net">Surya Suravarapu</a>
 * @version $Id$
 */
public class MemoryStore implements Store {

    /**
     * This number is magic. It was established using empirical testing of the two approaches
     * in CacheTest#testConcurrentReadWriteRemoveLFU. 5 is the cross over point
     * between the two algorithms. In future we ditch iteration entirely
     */
    protected static final int TOO_LARGE_TO_EFFICIENTLY_ITERATE = 5;

    /**
     * This is the default from {@link java.util.concurrent.ConcurrentHashMap}. It should never be used, because
     * we size the map to the max size of the store.
     */
    protected static final float DEFAULT_LOAD_FACTOR = .75f;

    /**
     * Set optimisation for 100 concurrent threads.
     */
    protected static final int CONCURRENCY_LEVEL = 100;

    private static final int JUMP_AHEAD = 5;

    private static final Logger LOG = Logger.getLogger(MemoryStore.class.getName());

    /**
     * The cache this store is associated with.
     */
    protected Ehcache cache;

    /**
     * when sampling elements, whether to iterate or to use the keySample array for faster random access
     */
    protected final boolean useKeySample;

    /**
     * Map where items are stored by key.
     */
    protected Map map;

    /**
     * The DiskStore associated with this MemoryStore.
     */
    protected final Store diskStore;

    /**
     * The maximum size of the store
     */
    protected final int maximumSize;

    /**
     * status.
     */
    protected volatile Status status;

    /**
     * The eviction policy to use
     */
    protected volatile Policy policy;

    private AtomicReferenceArray<Object> keyArray;
    private AtomicInteger keySamplePointer;

    /**
     * Constructs things that all MemoryStores have in common.
     *
     * @param cache
     * @param diskStore
     */
    protected MemoryStore(final Ehcache cache, final Store diskStore) {
        status = Status.STATUS_UNINITIALISED;
        this.cache = cache;
        this.maximumSize = cache.getCacheConfiguration().getMaxElementsInMemory();
        this.diskStore = diskStore;
        this.policy = determineEvictionPolicy();

        map = new ConcurrentHashMap(maximumSize, DEFAULT_LOAD_FACTOR, CONCURRENCY_LEVEL);
        if (maximumSize > TOO_LARGE_TO_EFFICIENTLY_ITERATE) {
            useKeySample = true;
            keyArray = new AtomicReferenceArray<Object>(maximumSize);
            keySamplePointer = new AtomicInteger(0);
        } else {
            useKeySample = false;
            keyArray = null;
            keySamplePointer = null;
        }

        status = Status.STATUS_ALIVE;

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Initialized " + this.getClass().getName() + " for " + cache.getName());
        }
    }


    /**
     * A factory method to create a MemoryStore.
     *
     * @param cache
     * @param diskStore
     * @return an instance of a MemoryStore, configured with the appropriate eviction policy
     */
    public static MemoryStore create(final Ehcache cache, final Store diskStore) {
        MemoryStore memoryStore = new MemoryStore(cache, diskStore);
        return memoryStore;
    }

    /**
     * Puts an item in the store. Note that this automatically results in an eviction if the store is full.
     *
     * @param element the element to add
     */
    public synchronized final void put(final Element element) throws CacheException {
        if (element != null) {
            map.put(element.getObjectKey(), element);
            doPut(element);
        }
    }

    /**
     * Gets an item from the cache.
     * <p/>
     * The last access time in {@link net.sf.ehcache.Element} is updated.
     *
     * @param key the key of the Element
     * @return the element, or null if there was no match for the key
     */
    public final Element get(final Object key) {

        if (key == null) {
            return null;
        }

        return (Element) map.get(key);
    }

    /**
     * Gets an item from the cache, without updating statistics.
     *
     * @param key the cache key
     * @return the element, or null if there was no match for the key
     */
    public final Element getQuiet(Object key) {
        return get(key);
    }

    /**
     * Removes an Element from the store.
     *
     * @param key the key of the Element, usually a String
     * @return the Element if one was found, else null
     */
    public final Element remove(final Object key) {

        if (key == null) {
            return null;
        }

        // remove single item.
        Element element = (Element)map.remove(key);
        if (element != null) {
            return element;
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, cache.getName() + "Cache: Cannot remove entry as key " + key + " was not found");
            }
            return null;
        }
    }

    /**
     * Memory stores are never backed up and always return false
     */
    public final boolean bufferFull() {
        return false;
    }

    /**
     * Expire all elements.
     * <p/>
     * This is a default implementation which does nothing. Expiration on demand is only
     * implemented for disk stores.
     */
    public void expireElements() {
        //empty implementation
    }

    /**
     * Chooses the Policy from the cache configuration
     * 
     */
    protected final Policy determineEvictionPolicy() {
        MemoryStoreEvictionPolicy policySelection = cache.getCacheConfiguration().getMemoryStoreEvictionPolicy();

        if (policySelection.equals(MemoryStoreEvictionPolicy.LRU)) {
            return new LruPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.FIFO)) {
            return new FifoPolicy();
        } else if (policySelection.equals(MemoryStoreEvictionPolicy.LFU)) {
            return new LfuPolicy();
        }

        throw new IllegalArgumentException(policySelection + " isn't a valid eviction policy");
    }

    /**
     * Remove all of the elements from the store.
     */
    public final void removeAll() throws CacheException {
        clear();
    }

    /**
     * Clears any data structures and places it back to its state when it was first created.
     */
    protected final void clear() {
        map.clear();
        //also clear sample as chances of producing a useful result after a removeAll are 0
        if (useKeySample) {
            //clear this. Because this is not locked, a few puts may get overwritten and be unable to be sample
            //for eviction. Not a problem.
            for (int i = 0; i < keyArray.length(); i++) {
                keyArray.set(i, null);
            }
        }
    }

    /**
     * Prepares for shutdown.
     */
    public final synchronized void dispose() {
        if (status.equals(Status.STATUS_SHUTDOWN)) {
            return;
        }
        status = Status.STATUS_SHUTDOWN;
        flush();

        //release reference to cache
        cache = null;
        map = null;
        keyArray = null;
        keySamplePointer = null;
    }

    /**
     * Flush to disk only if the cache is diskPersistent.
     */
    public final void flush() {
        if (cache.getCacheConfiguration().isDiskPersistent()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, cache.getName() + " is persistent. Spooling " + map.size() + " elements to the disk store.");
            }
            spoolAllToDisk();
        }

        //should be emptied if clearOnFlush is true
        if (cache.getCacheConfiguration().isClearOnFlush()) {
            clear();
        }
    }

    /**
     * Spools all elements to disk, in preparation for shutdown.
     * <p/>
     * This revised implementation is a little slower but avoids using increased memory during the method.
     */
    protected final void spoolAllToDisk() {
        boolean clearOnFlush = cache.getCacheConfiguration().isClearOnFlush();
        Object[] keys = getKeyArray();
        for (Object key : keys) {
            Element element = (Element) map.get(key);
            if (element != null) {
                if (!element.isSerializable()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Object with key " + element.getObjectKey()
                                + " is not Serializable and is not being overflowed to disk.");
                    }
                } else {
                    spoolToDisk(element);
                    //Don't notify listeners. They are not being removed from the cache, only a store
                    //Leave it in the memory store for performance if do not want to clear on flush
                    if (clearOnFlush) {
                        remove(key);
                    }
                }
            }
        }
    }

    /**
     * Puts the element in the DiskStore.
     * Should only be called if overflowToDisk is true
     * <p/>
     * Relies on being called from a synchronized method
     *
     * @param element The Element
     */
    protected void spoolToDisk(final Element element) {
        diskStore.put(element);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, cache.getName() + "Cache: spool to disk done for: " + element.getObjectKey());
        }
    }

    /**
     * Gets an Array of the keys for all elements in the memory cache.
     * <p/>
     * Does not check for expired entries
     *
     * @return An Object[]
     */
    public final Object[] getKeyArray() {
        return map.keySet().toArray();
    }

    /**
     * Returns the current store size.
     *
     * @return The size value
     */
    public final int getSize() {
        return map.size();
    }

    
    /**
     * Returns nothing since a disk store isn't clustered
     * @return returns 0
     */
    public final int getTerracottaClusteredSize() {
        return 0;
    }

    /**
     * A check to see if a key is in the Store. No check is made to see if the Element is expired.
     *
     * @param key The Element key
     * @return true if found. If this method return false, it means that an Element with the given key is definitely not in the MemoryStore.
     *         If it returns true, there is an Element there. An attempt to get it may return null if the Element has expired.
     */
    public final boolean containsKey(final Object key) {
        return map.containsKey(key);
    }


    /**
     * Measures the size of the memory store by measuring the serialized size of all elements.
     * If the objects are not Serializable they count as 0.
     * <p/>
     * Warning: This method can be very expensive to run. Allow approximately 1 second
     * per 1MB of entries. Running this method could create liveness problems
     * because the object lock is held for a long period
     *
     * @return the size, in bytes
     */
    public final long getSizeInBytes() throws CacheException {
        long sizeInBytes = 0;
        for (Object o : map.values()) {
            Element element = (Element) o;
            if (element != null) {
                sizeInBytes += element.getSerializedSize();
            }
        }
        return sizeInBytes;
    }


    /**
     * Evict the <code>Element</code>.
     * <p/>
     * Evict means that the <code>Element</code> is:
     * <ul>
     * <li>if, the store is diskPersistent, the <code>Element</code> is spooled to the DiskStore
     * <li>if not, the <code>Element</code> is removed.
     * </ul>
     *
     * @param element the <code>Element</code> to be evicted.
     */
    protected final void evict(final Element element) throws CacheException {
        boolean spooled = false;
        if (cache.getCacheConfiguration().isOverflowToDisk()) {
            if (!element.isSerializable()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, new StringBuffer("Object with key ").append(element.getObjectKey())
                            .append(" is not Serializable and cannot be overflowed to disk").toString());
                }
            } else {
                spoolToDisk(element);
                spooled = true;
            }
        }

        if (!spooled) {
            cache.getCacheEventNotificationService().notifyElementEvicted(element, false);
        }
    }

    /**
     * Before eviction elements are checked.
     *
     * @param element
     */
    protected final void notifyExpiry(final Element element) {
        cache.getCacheEventNotificationService().notifyElementExpiry(element, false);
    }

    /**
     * An algorithm to tell if the MemoryStore is at or beyond its carrying capacity.
     */
    protected final boolean isFull() {
        return map.size() > maximumSize;
    }

    /**
     * Package local access to the map for testing
     */
    Map getBackingMap() {
        return map;
    }


    /**
     * Puts an element into the store
     */
    protected void doPut(final Element elementJustAdded) {
        if (isFull()) {
            removeElementChosenByEvictionPolicy(elementJustAdded);
        }
        if (useKeySample) {
            saveKey(elementJustAdded);
        }

    }

    /**
     * Saves the key to our fast access AtomicReferenceArray
     * <p/>
     * We save the new key if:
     * <ol>
     * <li>
     * <li>
     * </ol>
     *
     * @param elementJustAdded the new element
     */
    protected void saveKey(final Element elementJustAdded) {
        int index = incrementIndex();
        Object key = keyArray.get(index);
        Element oldElement = null;
        if (key != null) {
            oldElement = (Element) map.get(key);
        }
        if (oldElement != null && !oldElement.isExpired()) {
            if (policy.compare(oldElement, elementJustAdded)) {
                //new one will always be more desirable for eviction as no gets yet, unless no gets on old one.
                //Consequence of this algorithm
                keyArray.set(index, elementJustAdded.getObjectKey());
            }
        } else {
            keyArray.set(index, elementJustAdded.getObjectKey());
        }

    }


    /**
     * A bounds-safe incrementer, which loops back to zero when it exceeds the array size.
     * <p/>
     * This method is not synchronized. It uses CAS and loops until is can set the value.
     */
    protected int incrementIndex() {
        int newVal;
        while (true) {
            int oldVal = keySamplePointer.get();
            newVal = oldVal + 1;
            if (newVal > keyArray.length() - 1) {
                newVal = 0;
            }
            if (keySamplePointer.compareAndSet(oldVal, newVal)) {
                return newVal;
            }
        }
    }


    /**
     * Removes the element chosen by the eviction policy
     *
     * @param elementJustAdded it is possible for this to be null
     */
    protected void removeElementChosenByEvictionPolicy(final Element elementJustAdded) {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Cache is full. Removing element ...");
        }

        Element element = findEvictionCandidate(elementJustAdded);
        if (element == null) {
            LOG.log(Level.FINE, "Eviction selection miss. Selected element is null");
            return;
        }

        // If the element is expired, remove
        if (element.isExpired()) {
            remove(element.getObjectKey());
            notifyExpiry(element);
            return;
        }

        evict(element);
        remove(element.getObjectKey());
    }

    /**
     * Find a "relatively" unused element, but not the element just added.
     */
    protected final Element findEvictionCandidate(final Element elementJustAdded) {
        Element element = null;

        //attempt quicker eviction
        if (useKeySample) {
            Element[] elements = sampleElementsViaKeyArray();
            //this can return null. Let the cache get bigger by one.
            element = policy.selectedBasedOnPolicy(elements, elementJustAdded);

            if (element != null) {
                return element;
            }

            //To avoid an expensive search via iterating through the CHM, which is very expensive
            //but it is guaranteed to not return null, which would cause a memory leak
            //iterate through our list, which is really fast
            //If we cannot evict in accordance in the algorithm, drop back to an eviction based on FIFO
            int startingIndex = keySamplePointer.get();
            //jump ahead of the puts to make sure we don't grab something that is very new
            int counter = startingIndex + JUMP_AHEAD;
            int failsafeCounter = maximumSize;
            while (true) {
                if (counter > keyArray.length() - 1) {
                    counter = 0;
                }
                element = (Element) map.get(keyArray.get(counter));
                if (element != null) {
                    return element;
                }
                counter++;
                //Should never happen. Failsafe.
                if (failsafeCounter-- < 0) {
                    return elementJustAdded;
                }
            }
        } else {
            //Using iterate technique
            Element[] elements = sampleElements(map.size());
            return policy.selectedBasedOnPolicy(elements, elementJustAdded);
        }
    }


    /**
     * Uses random numbers to sample the entire map.
     * <p/>
     * This implemenation uses a key array.
     *
     * @return a random sample of elements
     */
    protected Element[] sampleElementsViaKeyArray() {
        int[] indices = LfuPolicy.generateRandomSampleIndices(maximumSize);
        Element[] elements = new Element[indices.length];
        for (int i = 0; i < indices.length; i++) {
            Object key = keyArray.get(indices[i]);
            if (key == null) {
                continue;
            }
            elements[i] = (Element) map.get(key);
        }
        return elements;
    }

    /**
     * Uses random numbers to sample the entire map.
     * <p/>
     * This implementation uses the {@link ConcurrentHashMap} iterator.
     *
     * @return a random sample of elements
     */
    protected Element[] sampleElements(final int size) {
        int[] offsets = LfuPolicy.generateRandomSample(size);
        Element[] elements = new Element[offsets.length];
        Iterator iterator = map.values().iterator();
        for (int i = 0; i < offsets.length; i++) {
            for (int j = 0; j < offsets[i]; j++) {
                //fast forward
                try {
                    iterator.next();
                } catch (NoSuchElementException e) {
                    //e.printStackTrace();
                }
            }

            try {
                elements[i] = ((Element) iterator.next());
            } catch (NoSuchElementException e) {
                //e.printStackTrace();
            }
        }
        return elements;
    }

    /**
     * @return the active eviction policy.
     */
    public Policy getEvictionPolicy() {
        return policy;
    }

    /**
     * Sets the policy. Use this method to inject a custom policy. This can be done while the store is alive.
     *
     * @param policy a new policy to be used in evicting elements in this store
     */
    public void setEvictionPolicy(final Policy policy) {
        this.policy = policy;
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return null;
    }

    /**
     * Gets the status of the MemoryStore.
     */
    public final Status getStatus() {
        return status;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isCacheCoherent() {
        return false;
    }
}
