/**
 *  Copyright 2003-2006 Greg Luck
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
package net.sf.ehcache.hibernate;


import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.Timestamper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * EHCache plugin for Hibernate.
 * <p/>
 * EHCache uses a {@link net.sf.ehcache.store.MemoryStore} and a
 * {@link net.sf.ehcache.store.DiskStore}.
 * <p/>
 * The {@link net.sf.ehcache.store.DiskStore} requires that both keys and values be {@link java.io.Serializable}.
 * However the MemoryStore does not and in ehcache-1.2 nonSerializable Objects are permitted. They are discarded
 * if an attempt it made to overflow them to Disk or to replicate them to remote cache peers.
 * <p/>
 *
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @version $Id$
 */
public final class EhCache implements Cache {

    private static final Log LOG = LogFactory.getLog(EhCache.class);

    private static final int SIXTY_THOUSAND_MS = 60000;

    private final net.sf.ehcache.Cache cache;

    /**
     * Creates a new Hibernate pluggable cache by name.
     * <p/>
     * ehcache will look in ehcache.xml to load the configuration for the cache.
     * If the cache is not there, it will use the defaultCache settings. It is
     * always a good idea to specifically configure each cache.
     *
     * @param cache The backing ehcache cache.
     */
    public EhCache(net.sf.ehcache.Cache cache) {
        this.cache = cache;
    }

    /**
     * Gets a value of an element which matches the given key.
     *
     * @param key the key of the element to return.
     * @return The value placed into the cache with an earlier put, or null if not found or expired
     * @throws org.hibernate.cache.CacheException
     *
     */
    public final Object get(Object key) throws CacheException {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("key: " + key);
            }
            if (key == null) {
                return null;
            } else {
                Element element = cache.get(key);
                if (element == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Element for " + key + " is null");
                    }
                    return null;
                } else {
                    return element.getObjectValue();
                }
            }
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Gets an object from the cache.
     *
     * @param key an Object value
     * @return the Object, or null if not found
     * @throws CacheException
     */
    public final Object read(Object key) throws CacheException {
        return get(key);
    }


    /**
     * Updates an object in the cache, or if it does not exist, inserts it.
     *
     * @param key   an Object key
     * @param value an Object value
     * @throws CacheException if the {@link net.sf.ehcache.CacheManager} is shutdown or another {@link Exception} occurs.
     */
    public final void update(Object key, Object value) throws CacheException {
        put(key, value);
    }

    /**
     * Puts an object into the cache.
     *
     * @param key   an Object key
     * @param value an Object value
     * @throws CacheException if the {@link net.sf.ehcache.CacheManager} is shutdown or another {@link Exception} occurs.
     */
    public final void put(Object key, Object value) throws CacheException {
        try {
            Element element = new Element(key, value);
            cache.put(element);
        } catch (IllegalArgumentException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }

    }

    /**
     * Removes the element which matches the key.
     * <p/>
     * If no element matches, nothing is removed and no Exception is thrown.
     *
     * @param key the key of the element to remove
     * @throws CacheException
     */
    public final void remove(Object key) throws CacheException {
        try {
            cache.remove(key);
        } catch (ClassCastException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Remove all elements in the cache, but leave the cache in a useable state.
     *
     * @throws CacheException
     */
    public final void clear() throws CacheException {
        try {
            cache.removeAll();
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        } catch (IOException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Remove the cache and make it unuseable.
     *
     * @throws CacheException
     */
    public final void destroy() throws CacheException {
        try {
            cache.getCacheManager().removeCache(cache.getName());
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Calls to this method should perform their own synchronization.
     * It is provided for distributed caches.
     * <p/>
     * ehcache does not support distributed locking and therefore this method does nothing.
     */
    public final void lock(Object key) throws CacheException {
        //noop
    }

    /**
     * Calls to this method should perform their own synchronization.
     * <p/>
     * ehcache does not support distributed locking and therefore this method does nothing.
     */
    public final void unlock(Object key) throws CacheException {
        //noop
    }

    /**
     * Gets the next timestamp;
     */
    public final long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * Returns the lock timeout for this cache, which is 60s
     */
    public final int getTimeout() {
        // 60 second lock timeout
        return Timestamper.ONE_MS * SIXTY_THOUSAND_MS;
    }

    /**
     * @return the region name of the cache, which is the cache name in ehcache
     */
    public final String getRegionName() {
        return cache.getName();
    }

    /**
     * Warning: This method can be very expensive to run. Allow approximately 1 second
     * per 1MB of entries. Running this method could create liveness problems
     * because the object lock is held for a long period
     * <p/>
     *
     * @return the approximate size of memory ehcache is using for the MemoryStore for this cache
     */
    public final long getSizeInMemory() {
        try {
            return cache.calculateInMemorySize();
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * @return the number of elements in ehcache's MemoryStore
     */
    public final long getElementCountInMemory() {
        try {
            return cache.getMemoryStoreSize();
        } catch (net.sf.ehcache.CacheException ce) {
            throw new CacheException(ce);
        }
    }

    /**
     * @return the number of elements in ehcache's DiskStore. 0 is there is no DiskStore
     */
    public final long getElementCountOnDisk() {
        return cache.getDiskStoreSize();
    }


    /**
     * @return a copy of the cache Elements as a Map
     */
    public final Map toMap() {
        try {
            Map result = new HashMap();
            Iterator iter = cache.getKeys().iterator();
            while (iter.hasNext()) {
                Object key = iter.next();
                result.put(key, cache.get(key).getObjectValue());
            }
            return result;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * @return the region name, which is the cache name in ehcache
     */
    public final String toString() {
        return "EHCache(" + getRegionName() + ')';
    }

    /**
     * Package protected method used for testing
     */
    final net.sf.ehcache.Cache getBackingCache() {
        return cache;
    }

}
