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
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;


/**
 * An implementation of a LruMemoryStore.
 * <p/>
 * This uses {@link java.util.LinkedHashMap} as its backing map. It uses the {@link java.util.LinkedHashMap} LRU
 * feature. LRU for this implementation means least recently accessed.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class LruMemoryStore implements Store {

    private static final Logger LOG = LoggerFactory.getLogger(LruMemoryStore.class.getName());

    /**
     * The cache this store is associated with.
     */
    protected Ehcache cache;

    /**
     * Map where items are stored by key.
     */
    protected Map map;

    /**
     * The DiskStore associated with this MemoryStore.
     */
    protected final Store diskStore;

    /**
     * status.
     */
    protected Status status;

    /**
     * The maximum size of the store (0 == no limit)
     */
    protected int maximumSize;


    /**
     * Constructor for the LruMemoryStore object
     * The backing {@link java.util.LinkedHashMap} is created with LRU by access order.
     */
    public LruMemoryStore(Ehcache cache, Store diskStore) {
        status = Status.STATUS_UNINITIALISED;
        this.maximumSize = cache.getCacheConfiguration().getMaxElementsInMemory();
        this.cache = cache;
        this.diskStore = diskStore;
        map = new SpoolingLinkedHashMap();
        status = Status.STATUS_ALIVE;
    }


    /**
     * Puts an item in the cache. Note that this automatically results in
     * {@link net.sf.ehcache.store.LruMemoryStore.SpoolingLinkedHashMap#removeEldestEntry} being called.
     *
     * @param element the element to add
     */
    public final boolean put(Element element) throws CacheException {
        return putInternal(element, null);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return putInternal(element, writerManager);
    }

    private synchronized boolean putInternal(Element element, CacheWriterManager writerManager) throws CacheException {
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
     * Allow specialised actions over adding the element to the map.
     *
     * @param element
     */
    protected void doPut(Element element) throws CacheException {
        //empty
    }

    /**
     * Gets an item from the cache.
     * <p/>
     * The last access time in {@link net.sf.ehcache.Element} is updated.
     *
     * @param key the cache key
     * @return the element, or null if there was no match for the key
     */
    public final synchronized Element get(Object key) {
        return (Element) map.get(key);
    }

    /**
     * Gets an item from the cache, without updating statistics.
     *
     * @param key the cache key
     * @return the element, or null if there was no match for the key
     */
    public final synchronized Element getQuiet(Object key) {
        return get(key);
    }

    /**
     * Removes an Element from the store.
     *
     * @param key the key of the Element, usually a String
     * @return the Element if one was found, else null
     */
    public final Element remove(Object key) {
        return removeInternal(key, null);
    }

    /**
     * {@inheritDoc}
     */
    public final Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return removeInternal(key, writerManager);
    }

    private synchronized Element removeInternal(Object key, CacheWriterManager writerManager) throws CacheException {
        // remove single item.
        Element element = (Element) map.remove(key);
        if (writerManager != null) {
            writerManager.remove(new CacheEntry(key, element));
        }
        if (element != null) {
            return element;
        } else {
            return null;
        }
    }

    /**
     * Remove all of the elements from the store.
     */
    public final synchronized void removeAll() throws CacheException {
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
        Object[] keys = getKeyArray();
        for (Object key : keys) {
            Element element = (Element) map.get(key);
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
     * Should only be called if isOverflowToDisk is true
     * <p/>
     * Relies on being called from a synchronized method
     *
     * @param element The Element
     */
    protected void spoolToDisk(Element element) {
        diskStore.put(element);
        if (LOG.isDebugEnabled()) {
            LOG.debug(cache.getName() + "Cache: spool to disk done for: " + element.getObjectKey());
        }
    }

    /**
     * Gets the status of the MemoryStore.
     */
    public final Status getStatus() {
        return status;
    }

    /**
     * Gets an Array of the keys for all elements in the memory cache.
     * <p/>
     * Does not check for expired entries
     *
     * @return An Object[]
     */
    public final synchronized Object[] getKeyArray() {
        return map.keySet().toArray();
    }

    /**
     * Returns the current cache size.
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
     * An unsynchronized check to see if a key is in the Store. No check is made to see if the Element is expired.
     *
     * @param key The Element key
     * @return true if found. If this method return false, it means that an Element with the given key is definitely not in the MemoryStore.
     *         If it returns true, there is an Element there. An attempt to get it may return null if the Element has expired.
     */
    public final boolean containsKey(Object key) {
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
    public final synchronized long getSizeInBytes() throws CacheException {
        long sizeInBytes = 0;
        for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
            Element element = (Element) iterator.next();
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
    protected final void evict(Element element) throws CacheException {
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
    protected final void notifyExpiry(Element element) {
        cache.getCacheEventNotificationService().notifyElementExpiry(element, false);
    }


    /**
     * An algorithm to tell if the MemoryStore is at or beyond its carrying capacity.
     */
    protected final boolean isFull() {
        return maximumSize > 0 && map.size() > maximumSize;
    }

    /**
     * Expire all elsments.
     * <p/>
     * This is a default implementation which does nothing. Expiry on demand is only
     * implemented for disk stores.
     */
    public void expireElements() {
        //empty implementation
    }

    /**
     * Memory stores are never backed up and always return false
     */
    public boolean bufferFull() {
        return false;
    }


    /**
     * Package local access to the map for testing
     */
    Map getBackingMap() {
        return map;
    }

    /**
     * An extension of LinkedHashMap which overrides {@link #removeEldestEntry}
     * to persist cache entries to the auxiliary cache before they are removed.
     * <p/>
     * This implementation also provides LRU by access order.
     */
    public final class SpoolingLinkedHashMap extends java.util.LinkedHashMap {
        private static final int INITIAL_CAPACITY = 100;
        private static final float GROWTH_FACTOR = .75F;

        /**
         * Default constructor.
         * Will create an initial capacity of 100, a loading of .75 and
         * LRU by access order.
         */
        public SpoolingLinkedHashMap() {
            super(INITIAL_CAPACITY, GROWTH_FACTOR, true);
        }

        /**
         * Returns <tt>true</tt> if this map should remove its eldest entry.
         * This method is invoked by <tt>put</tt> and <tt>putAll</tt> after
         * inserting a new entry into the map.  It provides the implementer
         * with the opportunity to remove the eldest entry each time a new one
         * is added.  This is useful if the map represents a cache: it allows
         * the map to reduce memory consumption by deleting stale entries.
         * <p/>
         * Will return true if:
         * <ol>
         * <li> the element has expired
         * <li> the cache size is greater than the in-memory actual.
         * In this case we spool to disk before returning.
         * </ol>
         *
         * @param eldest The least recently inserted entry in the map, or if
         *               this is an access-ordered map, the least recently accessed
         *               entry.  This is the entry that will be removed it this
         *               method returns <tt>true</tt>.  If the map was empty prior
         *               to the <tt>put</tt> or <tt>putAll</tt> invocation resulting
         *               in this invocation, this will be the entry that was just
         *               inserted; in other words, if the map contains a single
         *               entry, the eldest entry is also the newest.
         * @return true if the eldest entry should be removed
         *         from the map; <tt>false</t> if it should be retained.
         */
        @Override
        protected final boolean removeEldestEntry(Map.Entry eldest) {
            Element element = (Element) eldest.getValue();
            return element != null && removeLeastRecentlyUsedElement(element);
        }

        /**
         * Relies on being called from a synchronized method
         *
         * @param element
         * @return true if the LRU element should be removed
         */
        private boolean removeLeastRecentlyUsedElement(Element element) throws CacheException {
            //check for expiry and remove before going to the trouble of spooling it
            if (element.isExpired()) {
                notifyExpiry(element);
                return true;
            }

            if (isFull()) {
                evict(element);
                return true;
            } else {
                return false;
            }

        }
    }


    /**
     * @return the current eviction policy. This may not be the configured policy, if it has been
     *         dynamically set.
     * @see #setEvictionPolicy(Policy)
     */
    public Policy getEvictionPolicy() {
        return new LruPolicy();
    }

    /**
     * Sets the eviction policy strategy. The Store will use a policy at startup. The store may allow changing
     * the eviction policy strategy dynamically. Otherwise implementations will throw an exception if this method
     * is called.
     *
     * @param policy the new policy
     */
    public void setEvictionPolicy(Policy policy) {
        throw new UnsupportedOperationException("This store is LRU only. It does not support changing the eviction" +
                " strategy.");
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterCoherent() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheCoherent() {
        return false;
    }
}

