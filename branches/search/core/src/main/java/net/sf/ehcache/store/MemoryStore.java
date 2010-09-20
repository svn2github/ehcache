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

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.store.chm.SelectableConcurrentHashMap;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A Store implementation suitable for fast, concurrent in memory stores. The policy is determined by that
 * configured in the cache.
 *
 * @author <a href="mailto:ssuravarapu@users.sourceforge.net">Surya Suravarapu</a>
 * @version $Id$
 */
public class MemoryStore extends AbstractStore implements CacheConfigurationListener {

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
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * Set optimisation for 100 concurrent threads.
     */
    protected static final int CONCURRENCY_LEVEL = 100;

    private static final int MAX_EVICTION_RATIO = 5;

    private static final Logger LOG = LoggerFactory.getLogger(MemoryStore.class.getName());

    /**
     * The cache this store is associated with.
     */
    protected Ehcache cache;

    /**
     * when sampling elements, whether to iterate or to use the keySample array for faster random access
     */
    protected volatile boolean useKeySample;

    /**
     * Map where items are stored by key.
     */
    protected SelectableConcurrentHashMap map;

    /**
     * The DiskStore associated with this MemoryStore.
     */
    protected final Store diskStore;

    /**
     * The maximum size of the store (0 == no limit)
     */
    protected volatile int maximumSize;

    /**
     * status.
     */
    protected volatile Status status;

    /**
     * The eviction policy to use
     */
    protected volatile Policy policy;

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

        // create the CHM with initialCapacity sufficient to hold maximumSize
        int initialCapacity = getInitialCapacityForLoadFactor(maximumSize, DEFAULT_LOAD_FACTOR);
        map = new SelectableConcurrentHashMap(initialCapacity, DEFAULT_LOAD_FACTOR, CONCURRENCY_LEVEL);
        if (maximumSize > TOO_LARGE_TO_EFFICIENTLY_ITERATE) {
            useKeySample = true;
        } else {
            useKeySample = false;
        }

        status = Status.STATUS_ALIVE;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
        }
    }

    /**
     * Calculates the initialCapacity for a desired maximumSize goal and loadFactor.
     *
     * @param maximumSizeGoal the desired maximum size goal
     * @param loadFactor      the load factor
     * @return the calculated initialCapacity. Returns 0 if the parameter <tt>maximumSizeGoal</tt> is less than or equal to 0
     */
    static int getInitialCapacityForLoadFactor(int maximumSizeGoal, float loadFactor) {
        double actualMaximum = Math.ceil(maximumSizeGoal / loadFactor);
        return Math.max(0, actualMaximum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) actualMaximum);
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
        cache.getCacheConfiguration().addConfigurationListener(memoryStore);
        return memoryStore;
    }

    /**
     * Puts an item in the store. Note that this automatically results in an eviction if the store is full.
     *
     * @param element the element to add
     */
    public final boolean put(final Element element) throws CacheException {
        return putInternal(element, null);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return putInternal(element, writerManager);
    }

    private boolean putInternal(Element element, CacheWriterManager writerManager) throws CacheException {
        boolean newPut = true;
        if (element != null) {
            newPut = map.put(element.getObjectKey(), element) == null;
            if (writerManager != null) {
                writerManager.put(element);
            }
            doPut(element);
        }
        return newPut;
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

        return map.get(key);
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
        return removeInternal(key, null);
    }

    /**
     * {@inheritDoc}
     */
    public final Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return removeInternal(key, writerManager);
    }

    private Element removeInternal(Object key, CacheWriterManager writerManager) throws CacheException {

        if (key == null) {
            return null;
        }

        // remove single item.
        Element element = map.remove(key);
        if (writerManager != null) {
            writerManager.remove(new CacheEntry(key, element));
        }
        if (element != null) {
            return element;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(cache.getName() + "Cache: Cannot remove entry as key " + key + " was not found");
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
    }

    /**
     * Flush to disk only if the cache is diskPersistent.
     */
    public final void flush() {
        if (cache.getCacheConfiguration().isDiskPersistent()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(cache.getName() + " is persistent. Spooling " + map.size() + " elements to the disk store.");
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
        for (Object key : getKeys()) {
            Element element = map.get(key);
            if (element != null) {
                if (!element.isSerializable()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Object with key " + element.getObjectKey()
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
        if (diskStore != null) {
            diskStore.put(element);
            if (LOG.isDebugEnabled()) {
                LOG.debug(cache.getName() + "Cache: spool to disk done for: " + element.getObjectKey());
            }
        }
    }

    /**
     * Gets an Array of the keys for all elements in the memory cache.
     * <p/>
     * Does not check for expired entries
     *
     * @return An List
     */
    public final List getKeys() {
        return new ArrayList(map.keySet());
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
     *
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug(new StringBuilder("Object with key ").append(element.getObjectKey())
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
        return maximumSize > 0 && map.size() > maximumSize;
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
        if (maximumSize > 0) {
            int evict = Math.min(map.quickSize() - maximumSize, MAX_EVICTION_RATIO);
            for (int i = 0; i < evict; i++) {
                removeElementChosenByEvictionPolicy(elementJustAdded);
            }
        }
    }

    /**
     * Removes the element chosen by the eviction policy
     *
     * @param elementJustAdded it is possible for this to be null
     */
    protected void removeElementChosenByEvictionPolicy(final Element elementJustAdded) {

        LOG.debug("Cache is full. Removing element ...");

        Element element = findEvictionCandidate(elementJustAdded);
        if (element == null) {
            LOG.debug("Eviction selection miss. Selected element is null");
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
        //attempt quicker eviction
        if (useKeySample) {
            Element[] elements = sampleElements(elementJustAdded.getObjectKey());
            //this can return null. Let the cache get bigger by one.
            return policy.selectedBasedOnPolicy(elements, elementJustAdded);
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
    protected Element[] sampleElements(Object keyHint) {
        int size = AbstractPolicy.calculateSampleSize(maximumSize);
        return map.getRandomValues(size, keyHint);
    }

    /**
     * Uses random numbers to sample the entire map.
     * <p/>
     * This implementation uses the {@link java.util.concurrent.ConcurrentHashMap} iterator.
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
    public void timeToIdleChanged(long oldTti, long newTti) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void timeToLiveChanged(long oldTtl, long newTtl) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void loggingChanged(boolean oldValue, boolean newValue) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        useKeySample = (newCapacity > TOO_LARGE_TO_EFFICIENTLY_ITERATE);
        maximumSize = newCapacity;
    }

    /**
     * {@inheritDoc}
     */
    public void registered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void deregistered(CacheConfiguration config) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return getEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return getSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        setEvictionPolicy(policy);
    }

    /**
     * Unsupported in MemoryStore
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported in MemoryStore
     */
    public Element removeElement(Element element) throws NullPointerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported in MemoryStore
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported in MemoryStore
     */
    public Element replace(Element element) throws NullPointerException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }
}

