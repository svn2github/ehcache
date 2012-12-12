/**
 *  Copyright Terracotta, Inc.
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
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.util.Timestamper;

import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EHCache plugin for Hibernate.
 * <p/>
 * EHCache uses a {@link net.sf.ehcache.store.MemoryStore} and a
 * {@link net.sf.ehcache.store.disk.DiskStore}.
 * <p/>
 * The {@link net.sf.ehcache.store.disk.DiskStore} requires that both keys and values be {@link java.io.Serializable}.
 * However the MemoryStore does not and in ehcache-1.2 nonSerializable Objects are permitted. They are discarded
 * if an attempt it made to overflow them to Disk or to replicate them to remote cache peers.
 * <p/>
 *
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @version $Id$
 */
@Deprecated
public final class EhCache implements Cache {

    private static final Logger LOG = LoggerFactory.getLogger(EhCache.class.getName());

    private static final int SIXTY_THOUSAND_MS = 60000;

    private final net.sf.ehcache.Ehcache cache;

    private final CacheLockProvider lockProvider;

    /**
     * Creates a new Hibernate pluggable cache by name.
     * <p/>
     * ehcache will look in ehcache.xml to load the configuration for the cache.
     * If the cache is not there, it will use the defaultCache settings. It is
     * always a good idea to specifically configure each cache.
     *
     * @param cache The backing ehcache cache.
     */
    public EhCache(net.sf.ehcache.Ehcache cache) {
        this.cache = cache;

        Object context = cache.getInternalContext();
        if (context instanceof CacheLockProvider) {
            lockProvider = (CacheLockProvider) context;
        } else {
            lockProvider = null;
        }
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
                LOG.debug("key: {}", key);
            if (key == null) {
                return null;
            } else {
                Element element = cache.get(key);
                if (element == null) {

                        LOG.debug("Element for key {} is null", key);
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
            LOG.debug("key: {} value: {}", key, value);
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
        }
    }

    /**
     * Remove the cache and make it unuseable.
     * <p/>
     *
     * @throws CacheException
     */
    public final void destroy() throws CacheException {
        try {
            cache.getCacheManager().removeCache(cache.getName());
        } catch (IllegalStateException e) {
            //When Spring and Hibernate are both involved this will happen in normal shutdown operation.
            //Do not throw an exception, simply log this one.

                LOG.debug("This can happen if multiple frameworks both try to shutdown ehcache", e);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void lock(Object key) throws CacheException {
        if (lockProvider != null) {
            lockProvider.getSyncForKey(key).lock(LockType.WRITE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void unlock(Object key) throws CacheException {
        if (lockProvider != null) {
            lockProvider.getSyncForKey(key).unlock(LockType.WRITE);
        }
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
            return cache.getStatistics().calculateInMemorySize();
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * @return the number of elements in ehcache's MemoryStore
     */
    public final long getElementCountInMemory() {
        try {
            return cache.getStatistics().getMemoryStoreSize();
        } catch (net.sf.ehcache.CacheException ce) {
            throw new CacheException(ce);
        }
    }

    /**
     * @return the number of elements in ehcache's DiskStore. 0 is there is no DiskStore
     */
    public final long getElementCountOnDisk() {
        return cache.getStatistics().getDiskStoreSize();
    }


    /**
     * @return a copy of the cache Elements as a Map
     */
    public final Map toMap() {
        try {
            Map result = new HashMap();
            for (Object key : cache.getKeys()) {
                Element e = cache.get(key);
                if (e != null) {
                    result.put(key, e.getObjectValue());
                }
            }
            return result;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * @return <code>true</code> if this cache supports entry locks.
     */
    public final boolean canLockEntries() {
        return lockProvider != null;
    }

    /**
     * @return the region name, which is the cache name in ehcache
     */
    @Override
    public final String toString() {
        return "EHCache(" + getRegionName() + ')';
    }

    /**
     * Package protected method used for testing
     */
    final net.sf.ehcache.Ehcache getBackingCache() {
        return cache;
    }

}
