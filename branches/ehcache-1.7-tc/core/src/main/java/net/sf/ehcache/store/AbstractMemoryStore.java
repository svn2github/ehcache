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

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * Abstract base class that provides common methods to facilitate the implementation of memory stores.
 *
 * @author <a href="mailto:gbevin@terracottatech.com">Geert Bevin</a>
 * @version $Id$
 */
public abstract class AbstractMemoryStore implements Store {

    private static final Logger LOG = Logger.getLogger(AbstractMemoryStore.class.getName());

    /**
     * The cache this store is associated with.
     */
    protected Ehcache cache;

    /**
     * Puts an item in the store. Note that this automatically results in an eviction if the store is full.
     *
     * @param element the element to add
     */
    public synchronized final void put(final Element element) throws CacheException {
        if (element != null) {
            putIntoBackend(element);
        }
    }

    /**
     * Puts the element in the data structure that backs the store, for example a map.
     *
     * This has to be implemented by all classes that extend this abstract base class.
     *
     * @param element the element to add
     */
    protected abstract void putIntoBackend(final Element element) throws CacheException;

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

        Element element = getFromBackend(key);

        if (element != null) {
            element.updateAccessStatistics();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(cache.getName() + "Cache: " + cache.getName() + "MemoryStore hit for " + key);
            }
        } else if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(cache.getName() + "Cache: " + cache.getName() + "MemoryStore miss for " + key);
        }

        return element;
    }

    /**
     * Gets the element from the data structure that backs the store, for example a map.
     *
     * This has to be implemented by all classes that extend this abstract base class.
     *
     * @param key the key of the Element
     * @return the element, or null if there was no match for the key
     */
    protected abstract Element getFromBackend(final Object key);

    /**
     * Gets an item from the cache, without updating statistics.
     *
     * @param key the key of the Element
     * @return the element, or null if there was no match for the key
     */
    public final Element getQuiet(final Object key) {
        Element cacheElement = getFromBackend(key);

        if (cacheElement != null) {
            //cacheElement.updateAccessStatistics(); Don't update statistics
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(cache.getName() + "Cache: " + cache.getName() + "MemoryStore hit for " + key);
            }
        } else if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(cache.getName() + "Cache: " + cache.getName() + "MemoryStore miss for " + key);
        }
        return cacheElement;
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
        Element element = removeFromBackend(key);
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
     * Removes the element from the data structure that backs the store, for example a map.
     *
     * This has to be implemented by all classes that extend this abstract base class.
     *
     * @param key the key of the Element
     * @return the Element if one was found, else null
     */
    protected abstract Element removeFromBackend(final Object key);

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
    public final void expireElements() {
        //empty implementation
    }

    /**
     * Chooses the Policy from the cache configuration
     *
     * @param cache
     */
    protected final Policy determineEvictionPolicy(final Ehcache cache) {
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
}
