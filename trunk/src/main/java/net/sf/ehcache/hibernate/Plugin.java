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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.hibernate.cache.CacheException;
import net.sf.hibernate.cache.Timestamper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;

/**
 * EHCache plugin for Hibernate
 *
 * EHCache uses a {@link net.sf.ehcache.store.LruMemoryStore} and a
 * {@link net.sf.ehcache.store.DiskStore}. The {@link net.sf.ehcache.store.DiskStore}
 * requires that both keys and values be {@link Serializable}. For this reason
 * this plugin throws Exceptions when either of these are not castable to {@link Serializable}.
 *
 * Because an ehcache plugin has been provided by the Hibernate project for quite some time, this class
 * is deprecated in favour of net.sf.hibernate.cache.EhCacheProvider 
 *
 * @version $Id: Plugin.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 * @author Greg Luck
 */
public class Plugin implements net.sf.hibernate.cache.Cache {
    private static final Log LOG = LogFactory.getLog(Plugin.class.getName());
    private static final int MS_PER_MINUTE = 60000;

    private Cache cache;

    /**
     * Creates a new Hibernate pluggable cache based on a cache name.
     * <p>
     * @param name the name of the cache. This cache must have already been configured.
     * @throws CacheException If there is no cache with the given name.
     * @deprecated Use net.sf.hibernate.cache.EhCacheProvider instead of this class.
     *
     */
    public Plugin(String name) throws CacheException {
        LOG.warn("This Hibernate plugin is deprecated. Consider changing to net.sf.hibernate.cache.EhCacheProvider.");
        try {
            CacheManager manager = CacheManager.getInstance();
            cache = manager.getCache(name);
            if (cache == null) {
                LOG.warn("Could not find configuration for " + name
                        + ". Configuring using the defaultCache settings.");
                manager.addCache(name);
                cache = manager.getCache(name);
            }
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Gets a value of an element which matches the given key.
     * @param key the key of the element to return.
     * @return The value placed into the cache with an earlier put, or null if not found or expired
     * @throws CacheException
     */
    public Object get(Object key) throws CacheException {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("key: " + key);
            }
            if (key == null) {
                return null;
            } else {
                Element element = cache.get((Serializable) key);
                if (element == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Element for " + key + " is null");
                    }
                    return null;
                } else {
                    return element.getValue();
                }
            }
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }


    /**
     * Puts an object into the cache.
     * @param key a {@link Serializable} key
     * @param value a {@link Serializable} value
     * @throws CacheException if the parameters are not {@link Serializable}, the {@link CacheManager}
     * is shutdown or another {@link Exception} occurs.
     */
    public void put(Object key, Object value) throws CacheException {
        try {
            Element element = new Element((Serializable) key, (Serializable) value);
            cache.put(element);
        } catch (IllegalArgumentException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }

    }

    /**
     * Removes the element which matches the key.
     * <p>
     * If no element matches, nothing is removed and no Exception is thrown.
     * @param key the key of the element to remove
     * @throws CacheException
     */
    public void remove(Object key) throws CacheException {
        try {
            cache.remove((Serializable) key);
        } catch (ClassCastException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Remove all elements in the cache, but leave the cache
     * in a useable state.
     * @throws CacheException
     */
    public void clear() throws CacheException {
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
     * @throws CacheException
     */
    public void destroy() throws CacheException {
        try {
            CacheManager.getInstance().removeCache(cache.getName());
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Calls to this method should perform there own synchronization.
     * It is provided for distributed caches. Because EHCache is not distributed
     * this method does nothing.
     */
    public void lock(Object key) throws CacheException {
    }

    /**
     * Calls to this method should perform there own synchronization.
     * It is provided for distributed caches. Because EHCache is not distributed
     * this method does nothing.
     */
    public void unlock(Object key) throws CacheException {
    }

    /**
     * Gets the next timestamp;
     */
    public long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * Returns the lock timeout for this cache.
     */
    public int getTimeout() {
        // 60 second lock timeout
        return Timestamper.ONE_MS * MS_PER_MINUTE;
    }

}
