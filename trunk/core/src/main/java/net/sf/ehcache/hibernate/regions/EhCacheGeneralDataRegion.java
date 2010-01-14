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
package net.sf.ehcache.hibernate.regions;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.GeneralDataRegion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An EhCache specific GeneralDataRegion.
 * <p>
 * GeneralDataRegion instances are used for both the timestamps and query caches.
 * 
 * @author Chris Dennis
 */
abstract class EhCacheGeneralDataRegion extends EhCacheDataRegion implements GeneralDataRegion {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheGeneralDataRegion.class);

    /**
     * Creates an EhCacheGeneralDataRegion using the given EhCache instance as a backing.
     */
    public EhCacheGeneralDataRegion(Ehcache cache) {
        super(cache);
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key) throws CacheException {
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
     * {@inheritDoc}
     */
    public void put(Object key, Object value) throws CacheException {
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
     * {@inheritDoc}
     */
    public void evict(Object key) throws CacheException {
        try {
            cache.remove(key);
        } catch (ClassCastException e) {
            throw new CacheException(e);
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evictAll() throws CacheException {
        try {
            cache.removeAll();
        } catch (IllegalStateException e) {
            throw new CacheException(e);
        }
    }
}
