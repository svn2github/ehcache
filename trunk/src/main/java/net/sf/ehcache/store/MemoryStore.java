/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.RegisteredEventListeners;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * An abstract class for the Memory Stores. All Memory store implementations for different
 * policies (e.g: FIFO, LFU, LRU, etc.) should extend this class.
 *
 * @author <a href="mailto:ssuravarapu@users.sourceforge.net">Surya Suravarapu</a>
 * @version $Id: MemoryStore.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public abstract class MemoryStore implements Store {

    private static final Log LOG = LogFactory.getLog(MemoryStore.class.getName());

    /**
     * The cache this store is associated with
     */
    protected Cache cache;

    /**
     * Map where items are stored by key
     */
    protected Map map;

    /**
     * The DiskStore associated with this MemoryStore
     */
    protected DiskStore diskStore;

    /**
     * status
     */
    protected Status status;

    /**
     * Constructs things that all MemoryStores have in common.
     *
     * @param cache
     * @param diskStore
     */
    protected MemoryStore(Cache cache, DiskStore diskStore) {
        status = Status.STATUS_UNINITIALISED;
        this.cache = cache;
        this.diskStore = diskStore;
        status = Status.STATUS_ALIVE;

        LOG.debug("Initialized " + this.getClass().getName() + " for " + cache.getName());
    }


    /**
     * A factory method to create a MemoryStore
     *
     * @param cache
     * @param diskStore
     * @return an instance of a MemoryStore, configured with the appropriate eviction policy
     */
    public static MemoryStore create(Cache cache, DiskStore diskStore) {
        MemoryStore memoryStore = null;
        MemoryStoreEvictionPolicy policy = cache.getMemoryStoreEvictionPolicy();

        if (policy.equals(MemoryStoreEvictionPolicy.LRU)) {
            memoryStore = new LruMemoryStore(cache, diskStore);
        } else if (policy.equals(MemoryStoreEvictionPolicy.FIFO)) {
            memoryStore = new FifoMemoryStore(cache, diskStore);
        } else if (policy.equals(MemoryStoreEvictionPolicy.LFU)) {
            memoryStore = new LfuMemoryStore(cache, diskStore);
        }
        return memoryStore;
    }

    /**
     * Puts an item in the cache. Note that this automatically results in
     * {@link net.sf.ehcache.store.LruMemoryStore.SpoolingLinkedHashMap#removeEldestEntry} being called.
     *
     * @param element the element to add
     */
    public synchronized void put(Element element) throws CacheException {
        if (element != null) {
            map.put(element.getKey(), element);
            doPut(element);
        }
    }

    /**
     * Allow specialised actions over adding the element to the map
     *
     * @param element
     */
    protected void doPut(Element element) throws CacheException {
        //empty
    }

    /**
     * Gets an item from the cache
     * <p/>
     * The last access time in {@link net.sf.ehcache.Element} is updated.
     *
     * @param key the cache key
     * @return the element, or null if there was no match for the key
     */
    public synchronized Element get(Serializable key) {
        Element element = (Element) map.get(key);

        if (element != null) {
            element.updateAccessStatistics();
            doGetOnFoundElement(element);
            if (LOG.isTraceEnabled()) {
                LOG.trace(cache.getName() + "Cache: " + cache.getName() + "MemoryStore hit for " + key);
            }
        } else if (LOG.isTraceEnabled()) {
            LOG.trace(cache.getName() + "Cache: " + cache.getName() + "MemoryStore miss for " + key);
        }
        return element;
    }

    /**
     * Allows subclasses to do any further processing when an element is found
     */
    protected void doGetOnFoundElement(Element element) {
        //nothing for this class
    }

    /**
     * Gets an item from the cache, without updating Element statistics
     *
     * @param key the cache key
     * @return the element, or null if there was no match for the key
     */
    public synchronized Element getQuiet(Serializable key) {
        Element cacheElement = (Element) map.get(key);

        if (cacheElement != null) {
            //cacheElement.updateAccessStatistics(); Don't update statistics
            if (LOG.isTraceEnabled()) {
                LOG.trace(cache.getName() + "Cache: " + cache.getName() + "MemoryStore hit for " + key);
            }
        } else if (LOG.isTraceEnabled()) {
            LOG.trace(cache.getName() + "Cache: " + cache.getName() + "MemoryStore miss for " + key);
        }
        return cacheElement;
    }


    /**
     * Removes an Element from the store.
     * @param key the key of the Element, usually a String
     * @return the Element if one was found, else null
     */
    public synchronized Element remove(Serializable key) {

        // remove single item.
        Element element = (Element) map.remove(key);
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
     * Remove all of the elements from the store.
     * <p/>
     * If there are registered <code>CacheEventListener</code>s they are notified of the expiry or removal
     * of the <code>Element</code> as each is removed.
     */
    public synchronized void removeAll() throws CacheException {
        notifyingRemoveAll();
        clear();
    }

    /**
     * Clears any data structures and places it back to its state when it was first created
     */
    protected void clear() {
        map.clear();
    }

    /**
     * If there are registered <code>CacheEventListener</code>s they are notified of the expiry or removal
     * of the <code>Element</code> as each is removed.
     */
    protected void notifyingRemoveAll() throws CacheException {
        RegisteredEventListeners listeners = cache.getCacheEventNotificationService();
        if (!listeners.getCacheEventListeners().isEmpty()) {
            Object[] keys = getKeyArray();
            for (int i = 0; i < keys.length; i++) {
                Serializable key = (Serializable) keys[i];
                Element element = remove(key);
                if (cache.isExpired(element)) {
                    listeners.notifyElementExpiry(element, false);
                } else {
                    listeners.notifyElementRemoved(element, false);
                }
            }
        }
    }

    /**
     * Prepares for shutdown.
     */
    public synchronized void dispose() {
        if (status.equals(Status.STATUS_SHUTDOWN)) {
            return;
        }
        status = Status.STATUS_SHUTDOWN;
        flush();

        //release reference to cache
        cache = null;
    }

    /**
     * flush to disk
     */
    public synchronized void flush() {
        if (cache.isOverflowToDisk()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(cache.getName() + " is persistent. Spooling " + map.size() + " elements to the disk store.");
            }
            spoolAllToDisk();
            clear();
        }
    }

    /**
     * Spools all elements to disk, in preparation for shutdown
     * <p/>
     * Relies on being called from a synchronized method
     */
    protected void spoolAllToDisk() {
        Collection values = map.values();
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            Element element = (Element) iterator.next();
            spoolToDisk(element);
        }
    }

    /**
     * Puts the element in the DiskStore
     * Should only be called if {@link Cache#isOverflowToDisk} is true
     * <p/>
     * Relies on being called from a synchronized method
     *
     * @param element The Element
     */
    protected void spoolToDisk(Element element) {
        try {
            diskStore.put(element);
        } catch (IOException e) {
            LOG.error("Error spooling to disk"  + ". Error was " + e.getMessage());
            throw new IllegalStateException(e.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(cache.getName() + "Cache: spool to disk done for: " + element.getKey());
        }
    }

    /**
     * Gets the status of the MemoryStore.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Gets an Array of the keys for all elements in the memory cache
     * <p/>
     * Does not check for expired entries
     *
     * @return An Object[]
     */
    public synchronized Object[] getKeyArray() {
        return map.keySet().toArray();
    }

    /**
     * Returns the current cache size.
     *
     * @return The size value
     */
    public int getSize() {
        return map.size();
    }


    /**
     * An unsynchronized check to see if a key is in the Store. No check is made to see if the Element is expired.
     *
     * @param key The Element key
     * @return true if found. If this method return false, it means that an Element with the given key is definitely not in the MemoryStore.
     *         If it returns true, there is an Element there. An attempt to get it may return null if the Element has expired.
     */
    public boolean containsKey(Serializable key) {
        return map.containsKey(key);
    }


    /**
     * Measures the size of the memory store by measuring the serialized
     * size of all elements.
     * <p/>
     * Warning: This method can be very expensive to run. Allow approximately 1 second
     * per 1MB of entries. Running this method could create liveness problems
     * because the object lock is held for a long period
     *
     * @return the size, in bytes
     */
    public synchronized long getSizeInBytes() throws CacheException {
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
    protected void evict(Element element) throws CacheException {
        if (cache.isOverflowToDisk()) {
            spoolToDisk(element);
        } else {
            cache.getCacheEventNotificationService().notifyElementRemoved(element, false);
        }
    }

    /**
     * Before eviction elements are checked
     *
     * @param element
     */
    protected void notifyExpiry(Element element) {
        cache.getCacheEventNotificationService().notifyElementExpiry(element, false);
    }

    /**
     * An algorithm to tell if the MemoryStore is at or beyond its carrying capacity
     */
    protected boolean isFull() {
        return map.size() > cache.getMaxElementsInMemory();
    }

}
